package com.aicc.silverlink.domain.call.service;

import com.aicc.silverlink.domain.call.dto.CallBotInternalDto.*;
import com.aicc.silverlink.domain.call.entity.*;
import com.aicc.silverlink.domain.call.repository.*;
import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.domain.elderly.repository.ElderlyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CallBot Internal API 서비스
 * Python CallBot에서 호출하여 통화 데이터를 저장하는 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CallBotInternalService {

    private final CallRecordRepository callRecordRepository;
    private final LlmModelRepository llmModelRepository;
    private final ElderlyResponseRepository elderlyResponseRepository;
    private final CallSummaryRepository callSummaryRepository;
    private final CallEmotionRepository callEmotionRepository;
    private final CallDailyStatusRepository callDailyStatusRepository;
    private final ElderlyRepository elderlyRepository;
    private final com.aicc.silverlink.global.sse.CallBotSseService sseService;

    // ========== 통화 시작 ==========

    public StartCallResponse startCall(StartCallRequest request) {
        Elderly elderly = elderlyRepository.findById(request.getElderlyId())
                .orElseThrow(() -> new IllegalArgumentException("어르신을 찾을 수 없습니다."));

        CallRecord callRecord = CallRecord.builder()
                .elderly(elderly)
                .callAt(request.getCallAt() != null ? request.getCallAt() : LocalDateTime.now())
                .callTimeSec(0)
                .state(CallState.ANSWERED)
                .build();

        callRecordRepository.save(callRecord);
        log.info("✅ [DB 저장] 통화 기록 생성 성공: callId={}, elderlyId={}, name={}", 
                callRecord.getId(), elderly.getId(), elderly.getUser().getName());

        return StartCallResponse.builder()
                .callId(callRecord.getId())
                .elderlyId(elderly.getId())
                .callAt(callRecord.getCallAt())
                .build();
    }

    // ========== LLM Prompt 저장 ==========

    public void savePrompt(Long callId, SavePromptRequest request) {
        CallRecord callRecord = getCallRecord(callId);

        LlmModel llmModel = LlmModel.builder()
                .callRecord(callRecord)
                .prompt(request.getPrompt())
                .build();

        llmModelRepository.save(llmModel);
        log.info("✅ [DB 저장] LLM 발화(Prompt) 저장 완료: callId={}, modelId={}", callId, llmModel.getId());

        sseService.broadcast(callId, "prompt", request.getPrompt());
    }

    // ========== 어르신 응답 저장 ==========

    public void saveReply(Long callId, SaveReplyRequest request) {
        CallRecord callRecord = getCallRecord(callId);

        LlmModel llmModel = llmModelRepository.findTopByCallRecordOrderByIdDesc(callRecord)
                .orElseThrow(() -> new IllegalArgumentException("이전 발화(Prompt)를 찾을 수 없습니다. callId=" + callId));

        ElderlyResponse response = ElderlyResponse.builder()
                .callRecord(callRecord)
                .llmModel(llmModel)
                .content(request.getContent())
                .danger(request.getDanger() != null && request.getDanger())
                .build();

        elderlyResponseRepository.save(response);
        log.info("✅ [DB 저장] 어르신 응답(Reply) 저장 완료: callId={}, responseId={}, danger={}", 
                callId, response.getId(), response.isDanger());

        sseService.broadcast(callId, "reply", request.getContent());
    }

    // ========== 대화 메시지 저장 (Unified) ==========

    public MessageResponse saveMessage(Long callId, MessageRequest request) {
        CallRecord callRecord = getCallRecord(callId);

        if ("CALLBOT".equalsIgnoreCase(request.getSpeaker())) {
            MessageResponse resp = saveCallBotMessage(callRecord, request);
            log.info("✅ [DB 저장] 메시지(BOT) 저장 성공: callId={}, msgId={}", callId, resp.getMessageId());
            return resp;
        } else if ("ELDERLY".equalsIgnoreCase(request.getSpeaker())) {
            MessageResponse resp = saveElderlyMessage(callRecord, request);
            log.info("✅ [DB 저장] 메시지(USER) 저장 성공: callId={}, msgId={}", callId, resp.getMessageId());
            return resp;
        } else {
            throw new IllegalArgumentException("speaker는 'CALLBOT' 또는 'ELDERLY'여야 합니다.");
        }
    }

    private MessageResponse saveCallBotMessage(CallRecord callRecord, MessageRequest request) {
        LlmModel llmModel = LlmModel.builder()
                .callRecord(callRecord)
                .prompt(request.getContent())
                .build();

        llmModelRepository.save(llmModel);
        return MessageResponse.builder()
                .messageId(llmModel.getId())
                .speaker("CALLBOT")
                .timestamp(llmModel.getCreatedAt())
                .build();
    }

    private MessageResponse saveElderlyMessage(CallRecord callRecord, MessageRequest request) {
        LlmModel llmModel = llmModelRepository.findFirstByCallRecordOrderByCreatedAtDesc(callRecord)
                .orElseThrow(() -> new IllegalArgumentException("연결할 CallBot 발화가 없습니다."));

        ElderlyResponse response = ElderlyResponse.builder()
                .llmModel(llmModel)
                .callRecord(callRecord)
                .content(request.getContent())
                .respondedAt(request.getTimestamp() != null ? request.getTimestamp() : LocalDateTime.now())
                .danger(request.getDanger() != null && request.getDanger())
                .dangerReason(request.getDangerReason())
                .build();

        elderlyResponseRepository.save(response);
        return MessageResponse.builder()
                .messageId(response.getId())
                .speaker("ELDERLY")
                .timestamp(response.getRespondedAt())
                .build();
    }

    // ========== 통화 요약 저장 ==========

    public SimpleResponse saveSummary(Long callId, SummaryRequest request) {
        CallRecord callRecord = getCallRecord(callId);
        callSummaryRepository.deleteByCallRecord(callRecord);

        CallSummary summary = CallSummary.builder()
                .callRecord(callRecord)
                .content(request.getContent())
                .build();

        callSummaryRepository.save(summary);
        log.info("✅ [DB 저장] 통화 요약 저장 완료: callId={}, summaryId={}", callId, summary.getId());

        return SimpleResponse.builder().success(true).message("요약 저장 완료").id(summary.getId()).build();
    }

    // ========== 감정 분석 저장 ==========

    public SimpleResponse saveEmotion(Long callId, EmotionRequest request) {
        CallRecord callRecord = getCallRecord(callId);
        callEmotionRepository.deleteByCallRecord(callRecord);

        EmotionLevel emotionLevel = EmotionLevel.valueOf(request.getEmotionLevel().toUpperCase());
        CallEmotion emotion = CallEmotion.builder()
                .callRecord(callRecord)
                .emotionLevel(emotionLevel)
                .build();

        callEmotionRepository.save(emotion);
        log.info("✅ [DB 저장] 감정 분석 저장 완료: callId={}, level={}", callId, emotionLevel);

        return SimpleResponse.builder().success(true).message("감정 저장 완료").id(emotion.getId()).build();
    }

    // ========== 일일 상태 저장 ==========

    public SimpleResponse saveDailyStatus(Long callId, DailyStatusRequest request) {
        CallRecord callRecord = getCallRecord(callId);
        callDailyStatusRepository.deleteByCallRecord(callRecord);

        CallDailyStatus dailyStatus = CallDailyStatus.builder()
                .callRecord(callRecord)
                .mealTaken(request.getMealTaken())
                .healthStatus(parseStatusLevel(request.getHealthStatus()))
                .healthDetail(request.getHealthDetail())
                .sleepStatus(parseStatusLevel(request.getSleepStatus()))
                .sleepDetail(request.getSleepDetail())
                .build();

        callDailyStatusRepository.save(dailyStatus);
        callRecord.setDailyStatus(dailyStatus);
        log.info("✅ [DB 저장] 일일 상태 저장 완료: callId={}, meal={}, health={}, sleep={}", 
                callId, request.getMealTaken(), request.getHealthStatus(), request.getSleepStatus());

        return SimpleResponse.builder().success(true).message("일일 상태 저장 완료").id(dailyStatus.getId()).build();
    }

    // ========== 통화 종료 ==========

    public SimpleResponse endCall(Long callId, EndCallRequest request) {
        CallRecord callRecord = getCallRecord(callId);
        callRecord.setRecordingUrl(request.getRecordingUrl());
        callRecord.setCallTimeSec(request.getCallTimeSec());
        
        // 상태 변경
        callRecord.updateState(CallState.COMPLETED);

        if (request.getSummary() != null) saveSummary(callId, request.getSummary());
        if (request.getEmotion() != null) saveEmotion(callId, request.getEmotion());
        if (request.getDailyStatus() != null) saveDailyStatus(callId, request.getDailyStatus());

        callRecordRepository.save(callRecord);
        log.info("🚀 [DB 최종확정] 통화 종료 처리 완료: callId={}, duration={}sec", callId, request.getCallTimeSec());

        return SimpleResponse.builder().success(true).message("통화 종료 처리 완료").id(callId).build();
    }

    private CallRecord getCallRecord(Long callId) {
        return callRecordRepository.findById(callId)
                .orElseThrow(() -> new IllegalArgumentException("통화 기록을 찾을 수 없습니다: " + callId));
    }

    private CallDailyStatus.StatusLevel parseStatusLevel(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return CallDailyStatus.StatusLevel.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public List<CallLogResponse> getCallLogs(Long callId) {
        getCallRecord(callId);
        List<CallLogResponse> logs = new java.util.ArrayList<>();
        List<LlmModel> prompts = llmModelRepository.findByCallIdOrderByCreatedAtAsc(callId);
        for (LlmModel p : prompts) {
            logs.add(CallLogResponse.builder().id(p.getId()).type("PROMPT").content(p.getPrompt()).timestamp(p.getCreatedAt()).build());
        }
        List<ElderlyResponse> replies = elderlyResponseRepository.findByCallRecordIdOrderByRespondedAtAsc(callId);
        for (ElderlyResponse r : replies) {
            logs.add(CallLogResponse.builder().id(r.getId()).type("REPLY").content(r.getContent()).timestamp(r.getRespondedAt()).build());
        }
        logs.sort(java.util.Comparator.comparing(CallLogResponse::getTimestamp));
        return logs;
    }
}