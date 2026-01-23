package com.aicc.silverlink.domain.call.repository;

import com.aicc.silverlink.domain.call.entity.ElderlyResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ElderlyResponseRepository extends JpaRepository<ElderlyResponse, Long> {

    /**
     * 통화 ID로 응답 목록 조회 (시간순)
     */
    List<ElderlyResponse> findByCallRecordIdOrderByRespondedAtAsc(Long callRecordId);

    /**
     * 통화 ID로 위험 응답만 조회
     */
    @Query("SELECT er FROM ElderlyResponse er WHERE er.callRecord.id = :callId AND er.danger = true")
    List<ElderlyResponse> findDangerResponsesByCallId(@Param("callId") Long callId);

    /**
     * 통화별 응답 개수
     */
    long countByCallRecordId(Long callRecordId);
}