package com.aicc.silverlink.domain.call.service;

import com.aicc.silverlink.domain.assignment.repository.AssignmentRepository;
import com.aicc.silverlink.domain.call.dto.CallReviewDto.*;
import com.aicc.silverlink.domain.call.entity.CallRecord;
import com.aicc.silverlink.domain.call.entity.CounselorCallReview;
import com.aicc.silverlink.domain.call.repository.*;
import com.aicc.silverlink.domain.counselor.entity.Counselor;
import com.aicc.silverlink.domain.counselor.repository.CounselorRepository;
import com.aicc.silverlink.domain.guardian.repository.GuardianElderlyRepository;
import com.aicc.silverlink.global.exception.BusinessException;
import com.aicc.silverlink.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CallReviewService {

    private final CallRecordRepository callRecordRepository;
    private final CounselorCallReviewRepository reviewRepository;
    private final ElderlyResponseRepository elderlyResponseRepository;
    private final CallSummaryRepository summaryRepository;
    private final CallEmotionRepository emotionRepository;
    private final CounselorRepository counselorRepository;
    private final AssignmentRepository assignmentRepository;
    private final GuardianElderlyRepository guardianElderlyRepository;

    // ===== 상담사용 메서드 =====

    /**
     * 상담사가 담당하는 어르신들의 통화 기록 목록 조회
     */
    public Page<CallRecordSummaryResponse> getCallRecordsForCounselor(Long counselorId, Pageable pageable) {
        validateCounselor(counselorId);

        Page<CallRecord> callRecords = callRecordRepository.findCallRecordsForCounselor(counselorId, pageable);

        List<CallRecordSummaryResponse> responses = callRecords.getContent().stream()
                .map(cr -> {
                    boolean reviewed = reviewRepository.existsByCallRecordIdAndCounselorId(cr.getId(), counselorId);
                    return CallRecordSummaryResponse.from(cr, reviewed);
                })
                .toList();

        return new PageImpl<>(responses, pageable, callRecords.getTotalElements());
    }

    /**
     * 통화 기록 상세 조회
     */
    public CallRecordDetailResponse getCallRecordDetail(Long callId, Long counselorId) {
        validateCounselor(counselorId);

        CallRecord callRecord = callRecordRepository.findByIdWithDetails(callId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CALL_RECORD_NOT_FOUND));

        // 상담사가 해당 어르신을 담당하는지 확인
        validateCounselorAssignment(counselorId, callRecord.getElderly().getId());

        // 응답 목록 조회 (Fetch Join에서 제외된 경우)
        if (callRecord.getElderlyResponses().isEmpty()) {
            callRecord.getElderlyResponses().addAll(
                    elderlyResponseRepository.findByCallRecordIdOrderByRespondedAtAsc(callId)
            );
        }

        CounselorCallReview review = reviewRepository.findByCallRecordIdAndCounselorId(callId, counselorId)
                .orElse(null);

        return CallRecordDetailResponse.from(callRecord, review);
    }

    /**
     * 상담사가 통화 리뷰 생성 (통화 확인 체크 + 코멘트)
     */
    @Transactional
    public ReviewResponse createReview(Long counselorId, ReviewRequest request) {
        Counselor counselor = validateCounselor(counselorId);

        CallRecord callRecord = callRecordRepository.findById(request.getCallId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CALL_RECORD_NOT_FOUND));

        // 상담사가 해당 어르신을 담당하는지 확인
        validateCounselorAssignment(counselorId, callRecord.getElderly().getId());

        // 이미 리뷰가 존재하는지 확인
        if (reviewRepository.existsByCallRecordIdAndCounselorId(request.getCallId(), counselorId)) {
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        CounselorCallReview review = CounselorCallReview.create(
                callRecord, counselor, request.getComment(), request.isUrgent()
        );

        CounselorCallReview savedReview = reviewRepository.save(review);
        log.info("상담사 통화 리뷰 생성: counselorId={}, callId={}, urgent={}",
                counselorId, request.getCallId(), request.isUrgent());

        return ReviewResponse.from(savedReview);
    }

    /**
     * 상담사가 통화 리뷰 수정
     */
    @Transactional
    public ReviewResponse updateReview(Long counselorId, Long reviewId, ReviewRequest request) {
        validateCounselor(counselorId);

        CounselorCallReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        // 본인의 리뷰인지 확인
        if (!review.getCounselor().getId().equals(counselorId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        review.updateComment(request.getComment(), request.isUrgent());
        log.info("상담사 통화 리뷰 수정: reviewId={}, counselorId={}", reviewId, counselorId);

        return ReviewResponse.from(review);
    }

    /**
     * 미확인 통화 건수 조회
     */
    public UnreviewedCountResponse getUnreviewedCount(Long counselorId) {
        validateCounselor(counselorId);

        long unreviewedCount = callRecordRepository.countUnreviewedCallsForCounselor(counselorId);
        long totalCount = callRecordRepository.findCallRecordsForCounselor(counselorId, Pageable.unpaged())
                .getTotalElements();

        return UnreviewedCountResponse.builder()
                .unreviewedCount(unreviewedCount)
                .totalCount(totalCount)
                .build();
    }

    // ===== 보호자용 메서드 =====

    /**
     * 보호자가 어르신의 통화 리뷰 목록 조회
     */
    public Page<GuardianCallReviewResponse> getCallReviewsForGuardian(Long guardianId, Long elderlyId, Pageable pageable) {
        // 보호자-어르신 관계 확인
        validateGuardianElderlyRelation(guardianId, elderlyId);

        Page<CounselorCallReview> reviews = reviewRepository.findReviewsByElderlyId(elderlyId, pageable);

        List<GuardianCallReviewResponse> responses = reviews.getContent().stream()
                .map(r -> GuardianCallReviewResponse.from(r.getCallRecord(), r))
                .toList();

        return new PageImpl<>(responses, pageable, reviews.getTotalElements());
    }

    /**
     * 보호자가 통화 상세 조회 (상담사 코멘트 포함)
     */
    public GuardianCallReviewResponse getCallDetailForGuardian(Long guardianId, Long callId) {
        CallRecord callRecord = callRecordRepository.findByIdWithDetails(callId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CALL_RECORD_NOT_FOUND));

        // 보호자-어르신 관계 확인
        validateGuardianElderlyRelation(guardianId, callRecord.getElderly().getId());

        // 리뷰가 있으면 함께 반환
        List<CounselorCallReview> reviews = reviewRepository.findByCallRecordIdOrderByReviewedAtDesc(callId);
        CounselorCallReview latestReview = reviews.isEmpty() ? null : reviews.get(0);

        return GuardianCallReviewResponse.from(callRecord, latestReview);
    }

    // ===== Private Helper Methods =====

    private Counselor validateCounselor(Long counselorId) {
        return counselorRepository.findById(counselorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private void validateCounselorAssignment(Long counselorId, Long elderlyId) {
        boolean isAssigned = assignmentRepository.existsByCounselorIdAndElderlyIdAndStatusActive(
                counselorId, elderlyId
        );
        if (!isAssigned) {
            throw new BusinessException(ErrorCode.NOT_ASSIGNED_ELDERLY);
        }
    }

    private void validateGuardianElderlyRelation(Long guardianId, Long elderlyId) {
        boolean hasRelation = guardianElderlyRepository.existsByGuardianIdAndElderlyId(guardianId, elderlyId);
        if (!hasRelation) {
            throw new BusinessException(ErrorCode.NOT_RELATED_ELDERLY);
        }
    }
}