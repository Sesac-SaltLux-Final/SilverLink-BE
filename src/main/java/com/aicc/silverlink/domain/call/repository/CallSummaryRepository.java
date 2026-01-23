package com.aicc.silverlink.domain.call.repository;

import com.aicc.silverlink.domain.call.entity.CallSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CallSummaryRepository extends JpaRepository<CallSummary, Long> {

    /**
     * 통화 ID로 요약 조회 (최신 1건)
     */
    Optional<CallSummary> findTopByCallRecordIdOrderByCreatedAtDesc(Long callRecordId);

    /**
     * 통화 ID로 모든 요약 조회
     */
    List<CallSummary> findByCallRecordIdOrderByCreatedAtDesc(Long callRecordId);
}