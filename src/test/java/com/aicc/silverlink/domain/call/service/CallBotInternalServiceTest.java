package com.aicc.silverlink.domain.call.service;

import com.aicc.silverlink.domain.call.dto.CallBotInternalDto.*;
import com.aicc.silverlink.domain.call.entity.*;
import com.aicc.silverlink.domain.call.repository.*;
import com.aicc.silverlink.domain.elderly.repository.ElderlyRepository;
import com.aicc.silverlink.global.sse.CallBotSseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("CallBotInternalService 단위 테스트")
class CallBotInternalServiceTest {

    @InjectMocks
    private CallBotInternalService callBotInternalService;

    @Mock
    private CallRecordRepository callRecordRepository;
    @Mock
    private LlmModelRepository llmModelRepository;
    @Mock
    private ElderlyResponseRepository elderlyResponseRepository;
    @Mock
    private CallSummaryRepository callSummaryRepository;
    @Mock
    private CallEmotionRepository callEmotionRepository;
    @Mock
    private CallDailyStatusRepository callDailyStatusRepository;
    @Mock
    private ElderlyRepository elderlyRepository;
    @Mock
    private CallBotSseService sseService;

    @Nested
    @DisplayName("통화 데이터 저장 (중복 방지 / 덮어씌우기)")
    class SaveDataWithOverwrite {

        @Test
        @DisplayName("통화 요약 저장 시 기존 데이터를 삭제하고 저장한다")
        void saveSummary() {
            // given
            Long callId = 1L;
            CallRecord callRecord = mock(CallRecord.class);
            SummaryRequest request = new SummaryRequest("새로운 요약입니다.");

            given(callRecordRepository.findById(callId)).willReturn(Optional.of(callRecord));

            // when
            callBotInternalService.saveSummary(callId, request);

            // then
            // 1. 기존 데이터 삭제 호출 검증
            verify(callSummaryRepository).deleteByCallRecord(callRecord);
            // 2. 새로운 데이터 저장 호출 검증
            verify(callSummaryRepository).save(any(CallSummary.class));
        }

        @Test
        @DisplayName("감정 분석 저장 시 기존 데이터를 삭제하고 저장한다")
        void saveEmotion() {
            // given
            Long callId = 1L;
            CallRecord callRecord = mock(CallRecord.class);
            EmotionRequest request = new EmotionRequest("GOOD");

            given(callRecordRepository.findById(callId)).willReturn(Optional.of(callRecord));

            // when
            callBotInternalService.saveEmotion(callId, request);

            // then
            // 1. 기존 데이터 삭제 호출 검증
            verify(callEmotionRepository).deleteByCallRecord(callRecord);
            // 2. 새로운 데이터 저장 호출 검증
            verify(callEmotionRepository).save(any(CallEmotion.class));
        }

        @Test
        @DisplayName("일일 상태 저장 시 기존 데이터를 삭제하고 저장한다")
        void saveDailyStatus() {
            // given
            Long callId = 1L;
            CallRecord callRecord = mock(CallRecord.class);
            DailyStatusRequest request = new DailyStatusRequest(
                    true, "GOOD", "건강함", "GOOD", "잘 잤음"
            );

            given(callRecordRepository.findById(callId)).willReturn(Optional.of(callRecord));

            // when
            callBotInternalService.saveDailyStatus(callId, request);

            // then
            // 1. 기존 데이터 삭제 호출 검증
            verify(callDailyStatusRepository).deleteByCallRecord(callRecord);
            // 2. 새로운 데이터 저장 호출 검증
            verify(callDailyStatusRepository).save(any(CallDailyStatus.class));
        }
    }
}
