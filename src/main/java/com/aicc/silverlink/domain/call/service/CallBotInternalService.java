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

    // ========== 통화 시작 ==========

    /**
     * 통화 시작 - CallRecord 생성
     */
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
        log.info("[CallBotInternal] 통화 시작: callId={}, elderlyId={}", callRecord.getId(), elderly.getId());

        return StartCallResponse.builder()
                .callId(callRecord.getId())
                .elderlyId(elderly.getId())
                .callAt(callRecord.getCallAt())
                .build();
    }

    // ========== 대화 메시지 저장 ==========

    /**
     * 대화 메시지 저장 (CallBot 발화 또는 어르신 응답)
     */
    public MessageResponse saveMessage(Long callId, MessageRequest request) {
        CallRecord callRecord = getCallRecord(callId);

        if ("CALLBOT".equalsIgnoreCase(request.getSpeaker())) {
            return saveCallBotMessage(callRecord, request);
        } else if ("ELDERLY".equalsIgnoreCase(request.getSpeaker())) {
            return saveElderlyMessage(callRecord, request);
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
        log.debug("[CallBotInternal] CallBot 발화 저장: callId={}, modelId={}", callRecord.getId(), llmModel.getId());

        return MessageResponse.builder()
                .messageId(llmModel.getId())
                .speaker("CALLBOT")
                .timestamp(llmModel.getCreatedAt())
                .build();
    }

    private MessageResponse saveElderlyMessage(CallRecord callRecord, MessageRequest request) {
        // LlmModel 연결 (선택)
        LlmModel llmModel = null;
        if (request.getLlmModelId() != null) {
            llmModel = llmModelRepository.findById(request.getLlmModelId())
                    .orElse(null);
        }

        // 연결할 LlmModel이 없으면 가장 최근 것 사용
        if (llmModel == null) {
            llmModel = llmModelRepository.findFirstByCallRecordOrderByCreatedAtDesc(callRecord)
                    .orElseThrow(() -> new IllegalArgumentException("연결할 CallBot 발화가 없습니다."));
        }

        ElderlyResponse response = ElderlyResponse.builder()
                .llmModel(llmModel)
                .callRecord(callRecord)
                .content(request.getContent())
                .respondedAt(request.getTimestamp() != null ? request.getTimestamp() : LocalDateTime.now())
                .danger(request.getDanger() != null && request.getDanger())
                .dangerReason(request.getDangerReason())
                .build();

        elderlyResponseRepository.save(response);
        log.debug("[CallBotInternal] 어르신 응답 저장: callId={}, responseId={}, danger={}",
                callRecord.getId(), response.getId(), response.isDanger());

        return MessageResponse.builder()
                .messageId(response.getId())
                .speaker("ELDERLY")
                .timestamp(response.getRespondedAt())
                .build();
    }

    // ========== 통화 요약 저장 ==========

    /**
     * 통화 요약 저장
     */
    public SimpleResponse saveSummary(Long callId, SummaryRequest request) {
        CallRecord callRecord = getCallRecord(callId);

        CallSummary summary = CallSummary.builder()
                .callRecord(callRecord)
                .content(request.getContent())
                .build();

        callSummaryRepository.save(summary);
        log.info("[CallBotInternal] 통화 요약 저장: callId={}, summaryId={}", callId, summary.getId());

        return SimpleResponse.builder()
                .success(true)
                .message("통화 요약 저장 완료")
                .id(summary.getId())
                .build();
    }

    // ========== 감정 분석 저장 ==========

    /**
     * 감정 분석 저장
     */
    public SimpleResponse saveEmotion(Long callId, EmotionRequest request) {
        CallRecord callRecord = getCallRecord(callId);

        EmotionLevel emotionLevel = EmotionLevel.valueOf(request.getEmotionLevel().toUpperCase());

        CallEmotion emotion = CallEmotion.builder()
                .callRecord(callRecord)
                .emotionLevel(emotionLevel)
                .build();

        callEmotionRepository.save(emotion);
        log.info("[CallBotInternal] 감정 분석 저장: callId={}, emotionLevel={}", callId, emotionLevel);

        return SimpleResponse.builder()
                .success(true)
                .message("감정 분석 저장 완료")
                .id(emotion.getId())
                .build();
    }

    // ========== 일일 상태 저장 ==========

    /**
     * 일일 상태 저장
     */
    public SimpleResponse saveDailyStatus(Long callId, DailyStatusRequest request) {
        CallRecord callRecord = getCallRecord(callId);

        // 기존 상태가 있으면 삭제 후 재생성
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

        log.info("[CallBotInternal] 일일 상태 저장: callId={}, meal={}, health={}, sleep={}",
                callId, request.getMealTaken(), request.getHealthStatus(), request.getSleepStatus());

        return SimpleResponse.builder()
                .success(true)
                .message("일일 상태 저장 완료")
                .id(dailyStatus.getId())
                .build();
    }

    // ========== 통화 종료 ==========

    /**
     * 통화 종료 처리
     */
    public SimpleResponse endCall(Long callId, EndCallRequest request) {
        CallRecord callRecord = getCallRecord(callId);

        // 통화 시간 및 녹음 URL 업데이트
        callRecord.setRecordingUrl(request.getRecordingUrl());
        // 상태 변경 (IN_PROGRESS -> COMPLETED)
        // 참고: CallRecord에 상태 변경 메서드가 없으면 추가 필요

        // 요약 저장 (있으면)
        if (request.getSummary() != null && request.getSummary().getContent() != null) {
            saveSummary(callId, request.getSummary());
        }

        // 감정 저장 (있으면)
        if (request.getEmotion() != null && request.getEmotion().getEmotionLevel() != null) {
            saveEmotion(callId, request.getEmotion());
        }

        // 일일 상태 저장 (있으면)
        if (request.getDailyStatus() != null) {
            saveDailyStatus(callId, request.getDailyStatus());
        }

        callRecordRepository.save(callRecord);
        log.info("[CallBotInternal] 통화 종료: callId={}, duration={}sec", callId, request.getCallTimeSec());

        return SimpleResponse.builder()
                .success(true)
                .message("통화 종료 처리 완료")
                .id(callId)
                .build();
    }

    // ========== Private Methods ==========

    private CallRecord getCallRecord(Long callId) {
        return callRecordRepository.findById(callId)
                .orElseThrow(() -> new IllegalArgumentException("통화 기록을 찾을 수 없습니다: " + callId));
    }

    private CallDailyStatus.StatusLevel parseStatusLevel(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return CallDailyStatus.StatusLevel.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("[CallBotInternal] 잘못된 상태 값: {}", status);
            return null;
        }
    }
}
