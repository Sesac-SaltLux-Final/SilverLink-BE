package com.aicc.silverlink.domain.call.service;

import com.aicc.silverlink.domain.assignment.repository.AssignmentRepository;
import com.aicc.silverlink.domain.call.dto.CallReviewDto.*;
import com.aicc.silverlink.domain.call.entity.*;
import com.aicc.silverlink.domain.call.repository.*;
import com.aicc.silverlink.domain.counselor.entity.Counselor;
import com.aicc.silverlink.domain.counselor.repository.CounselorRepository;
import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.domain.guardian.repository.GuardianElderlyRepository;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CallReviewService 단위 테스트")
class CallReviewServiceTest {

    @InjectMocks
    private CallReviewService callReviewService;

    @Mock
    private CallRecordRepository callRecordRepository;
    @Mock
    private CounselorCallReviewRepository reviewRepository;
    @Mock
    private ElderlyResponseRepository elderlyResponseRepository;
    @Mock
    private CallSummaryRepository summaryRepository;
    @Mock
    private CallEmotionRepository emotionRepository;
    @Mock
    private CounselorRepository counselorRepository;
    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private GuardianElderlyRepository guardianElderlyRepository;

    // ===== Helper Methods =====

    private User createMockCounselorUser() {
        User user = mock(User.class);
        doReturn("김상담").when(user).getName();
        doReturn("010-1234-5678").when(user).getPhone();
        return user;
    }

    private User createMockElderlyUser() {
        User user = mock(User.class);
        doReturn("박어르신").when(user).getName();
        doReturn("010-8765-4321").when(user).getPhone();
        return user;
    }

    private Counselor createMockCounselor(Long id) {
        Counselor counselor = mock(Counselor.class);
        doReturn(id).when(counselor).getId();
        doReturn(createMockCounselorUser()).when(counselor).getUser();
        return counselor;
    }

    private Elderly createMockElderly(Long id) {
        Elderly elderly = mock(Elderly.class);
        doReturn(id).when(elderly).getId();
        doReturn(createMockElderlyUser()).when(elderly).getUser();
        doReturn(75).when(elderly).age();
        doReturn(Elderly.Gender.F).when(elderly).getGender();
        return elderly;
    }

    private CallRecord createMockCallRecord(Long id, Elderly elderly) {
        CallRecord callRecord = mock(CallRecord.class);
        doReturn(id).when(callRecord).getId();
        doReturn(elderly).when(callRecord).getElderly();
        doReturn(LocalDateTime.now().minusHours(1)).when(callRecord).getCallAt();
        doReturn(180).when(callRecord).getCallTimeSec();
        doReturn(CallState.COMPLETED).when(callRecord).getState();
        doReturn("3:00").when(callRecord).getFormattedDuration();
        doReturn(new ArrayList<>()).when(callRecord).getElderlyResponses();
        doReturn(new ArrayList<>()).when(callRecord).getSummaries();
        doReturn(new ArrayList<>()).when(callRecord).getEmotions();
        doReturn(false).when(callRecord).hasDangerResponse();
        return callRecord;
    }

    private CounselorCallReview createMockReview(Long id, CallRecord callRecord, Counselor counselor) {
        CounselorCallReview review = mock(CounselorCallReview.class);
        doReturn(id).when(review).getId();
        doReturn(callRecord).when(review).getCallRecord();
        doReturn(counselor).when(review).getCounselor();
        doReturn(LocalDateTime.now()).when(review).getReviewedAt();
        doReturn("어르신 상태 양호합니다.").when(review).getComment();
        doReturn(false).when(review).isUrgent();
        doReturn(LocalDateTime.now()).when(review).getCreatedAt();
        doReturn(LocalDateTime.now()).when(review).getUpdatedAt();
        return review;
    }

    @Nested
    @DisplayName("통화 목록 조회")
    class GetCallRecordsForCounselor {

        @Test
        @DisplayName("상담사가 담당 어르신의 통화 목록을 조회한다")
        void success() {
            // given
            Long counselorId = 1L;
            Long elderlyId = 100L;
            Long callId = 1000L;
            Pageable pageable = PageRequest.of(0, 20);

            Counselor counselor = createMockCounselor(counselorId);
            Elderly elderly = createMockElderly(elderlyId);
            CallRecord callRecord = createMockCallRecord(callId, elderly);

            Page<CallRecord> callRecordPage = new PageImpl<>(List.of(callRecord));

            given(counselorRepository.findById(counselorId)).willReturn(Optional.of(counselor));
            given(callRecordRepository.findCallRecordsForCounselor(counselorId, pageable)).willReturn(callRecordPage);
            given(reviewRepository.existsByCallRecordIdAndCounselorId(callId, counselorId)).willReturn(true);

            // when
            Page<CallRecordSummaryResponse> result = callReviewService.getCallRecordsForCounselor(counselorId, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getCallId()).isEqualTo(callId);
            assertThat(result.getContent().get(0).isReviewed()).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 상담사로 조회 시 예외 발생")
        void failWithInvalidCounselor() {
            // given
            Long invalidCounselorId = 999L;
            Pageable pageable = PageRequest.of(0, 20);
            given(counselorRepository.findById(invalidCounselorId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> callReviewService.getCallRecordsForCounselor(invalidCounselorId, pageable))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("통화 리뷰 생성")
    class CreateReview {

        @Test
        @DisplayName("상담사가 통화 리뷰를 생성한다")
        void success() {
            // given
            Long counselorId = 1L;
            Long elderlyId = 100L;
            Long callId = 1000L;

            Counselor counselor = createMockCounselor(counselorId);
            Elderly elderly = createMockElderly(elderlyId);
            CallRecord callRecord = createMockCallRecord(callId, elderly);
            CounselorCallReview savedReview = createMockReview(1L, callRecord, counselor);

            ReviewRequest request = ReviewRequest.builder()
                    .callId(callId)
                    .comment("어르신께서 오늘 기분이 좋아보이셨습니다.")
                    .urgent(false)
                    .build();

            given(counselorRepository.findById(counselorId)).willReturn(Optional.of(counselor));
            given(callRecordRepository.findById(callId)).willReturn(Optional.of(callRecord));
            given(assignmentRepository.existsByCounselorIdAndElderlyIdAndStatusActive(counselorId, elderlyId)).willReturn(true);
            given(reviewRepository.existsByCallRecordIdAndCounselorId(callId, counselorId)).willReturn(false);
            given(reviewRepository.save(any(CounselorCallReview.class))).willReturn(savedReview);

            // when
            ReviewResponse result = callReviewService.createReview(counselorId, request);

            // then
            assertThat(result.getReviewId()).isEqualTo(1L);
            assertThat(result.getCallId()).isEqualTo(callId);
            verify(reviewRepository).save(any(CounselorCallReview.class));
        }

        @Test
        @DisplayName("이미 리뷰가 존재하면 예외 발생")
        void failWithDuplicateReview() {
            // given
            Long counselorId = 1L;
            Long elderlyId = 100L;
            Long callId = 1000L;

            Counselor counselor = createMockCounselor(counselorId);
            Elderly elderly = createMockElderly(elderlyId);
            CallRecord callRecord = createMockCallRecord(callId, elderly);

            ReviewRequest request = ReviewRequest.builder()
                    .callId(callId)
                    .comment("중복 리뷰")
                    .build();

            given(counselorRepository.findById(counselorId)).willReturn(Optional.of(counselor));
            given(callRecordRepository.findById(callId)).willReturn(Optional.of(callRecord));
            given(assignmentRepository.existsByCounselorIdAndElderlyIdAndStatusActive(counselorId, elderlyId)).willReturn(true);
            given(reviewRepository.existsByCallRecordIdAndCounselorId(callId, counselorId)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> callReviewService.createReview(counselorId, request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("담당하지 않는 어르신의 통화에 리뷰 작성 시 예외 발생")
        void failWithUnassignedElderly() {
            // given
            Long counselorId = 1L;
            Long elderlyId = 100L;
            Long callId = 1000L;

            Counselor counselor = createMockCounselor(counselorId);
            Elderly elderly = createMockElderly(elderlyId);
            CallRecord callRecord = createMockCallRecord(callId, elderly);

            ReviewRequest request = ReviewRequest.builder()
                    .callId(callId)
                    .comment("권한 없음")
                    .build();

            given(counselorRepository.findById(counselorId)).willReturn(Optional.of(counselor));
            given(callRecordRepository.findById(callId)).willReturn(Optional.of(callRecord));
            given(assignmentRepository.existsByCounselorIdAndElderlyIdAndStatusActive(counselorId, elderlyId)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> callReviewService.createReview(counselorId, request))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("통화 리뷰 수정")
    class UpdateReview {

        @Test
        @DisplayName("상담사가 본인의 리뷰를 수정한다")
        void success() {
            // given
            Long counselorId = 1L;
            Long reviewId = 1L;
            Long elderlyId = 100L;
            Long callId = 1000L;

            Counselor counselor = createMockCounselor(counselorId);
            Elderly elderly = createMockElderly(elderlyId);
            CallRecord callRecord = createMockCallRecord(callId, elderly);
            CounselorCallReview review = createMockReview(reviewId, callRecord, counselor);

            ReviewRequest request = ReviewRequest.builder()
                    .callId(callId)
                    .comment("수정된 코멘트입니다.")
                    .urgent(true)
                    .build();

            given(counselorRepository.findById(counselorId)).willReturn(Optional.of(counselor));
            given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

            // when
            ReviewResponse result = callReviewService.updateReview(counselorId, reviewId, request);

            // then
            verify(review).updateComment("수정된 코멘트입니다.", true);
        }

        @Test
        @DisplayName("다른 상담사의 리뷰 수정 시 예외 발생")
        void failWithOtherCounselorReview() {
            // given
            Long counselorId = 1L;
            Long otherCounselorId = 2L;
            Long reviewId = 2L;

            Counselor counselor = createMockCounselor(counselorId);
            Counselor otherCounselor = createMockCounselor(otherCounselorId);

            Elderly elderly = createMockElderly(100L);
            CallRecord callRecord = createMockCallRecord(1000L, elderly);
            CounselorCallReview otherReview = createMockReview(reviewId, callRecord, otherCounselor);

            ReviewRequest request = ReviewRequest.builder()
                    .callId(1000L)
                    .comment("수정 시도")
                    .build();

            given(counselorRepository.findById(counselorId)).willReturn(Optional.of(counselor));
            given(reviewRepository.findById(reviewId)).willReturn(Optional.of(otherReview));

            // when & then
            assertThatThrownBy(() -> callReviewService.updateReview(counselorId, reviewId, request))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("보호자용 통화 리뷰 조회")
    class GuardianCallReview {

        @Test
        @DisplayName("보호자가 어르신의 통화 리뷰를 조회한다")
        void success() {
            // given
            Long guardianId = 50L;
            Long elderlyId = 100L;
            Long counselorId = 1L;
            Long callId = 1000L;
            Pageable pageable = PageRequest.of(0, 20);

            Counselor counselor = createMockCounselor(counselorId);
            Elderly elderly = createMockElderly(elderlyId);
            CallRecord callRecord = createMockCallRecord(callId, elderly);
            CounselorCallReview review = createMockReview(1L, callRecord, counselor);

            Page<CounselorCallReview> reviewPage = new PageImpl<>(List.of(review));

            given(guardianElderlyRepository.existsByGuardianIdAndElderlyId(guardianId, elderlyId)).willReturn(true);
            given(reviewRepository.findReviewsByElderlyId(elderlyId, pageable)).willReturn(reviewPage);

            // when
            Page<GuardianCallReviewResponse> result = callReviewService.getCallReviewsForGuardian(guardianId, elderlyId, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getCounselorName()).isEqualTo("김상담");
        }

        @Test
        @DisplayName("보호 관계가 없는 어르신 조회 시 예외 발생")
        void failWithUnrelatedElderly() {
            // given
            Long guardianId = 50L;
            Long elderlyId = 999L;
            Pageable pageable = PageRequest.of(0, 20);

            given(guardianElderlyRepository.existsByGuardianIdAndElderlyId(guardianId, elderlyId)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> callReviewService.getCallReviewsForGuardian(guardianId, elderlyId, pageable))
                    .isInstanceOf(BusinessException.class);
        }
    }
}