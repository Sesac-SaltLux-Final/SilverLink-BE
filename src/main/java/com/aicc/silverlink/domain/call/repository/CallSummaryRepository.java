package com.aicc.silverlink.domain.call.repository;

import com.aicc.silverlink.domain.call.entity.CallSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CallSummaryRepository extends JpaRepository<CallSummary, Long> {

    /**
     * 특정 통화의 최신 요약
     */
    @Query("SELECT s FROM CallSummary s WHERE s.callRecord.id = :callId ORDER BY s.createdAt DESC LIMIT 1")
    Optional<CallSummary> findLatestByCallId(@Param("callId") Long callId);

    /**
     * 특정 통화의 요약 목록 (최신순)
     */
    List<CallSummary> findByCallRecordIdOrderByCreatedAtDesc(Long callId);

    /**
     * 특정 어르신의 최근 통화 요약들
     */
    @Query("SELECT s FROM CallSummary s " +
            "JOIN s.callRecord cr " +
            "WHERE cr.elderly.id = :elderlyId " +
            "ORDER BY s.createdAt DESC")
    List<CallSummary> findRecentByElderlyId(@Param("elderlyId") Long elderlyId);
}