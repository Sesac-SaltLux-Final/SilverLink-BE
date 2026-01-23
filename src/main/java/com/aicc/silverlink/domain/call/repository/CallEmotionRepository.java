package com.aicc.silverlink.domain.call.repository;

import com.aicc.silverlink.domain.call.entity.CallEmotion;
import com.aicc.silverlink.domain.call.entity.EmotionLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CallEmotionRepository extends JpaRepository<CallEmotion, Long> {

    /**
     * 통화 ID로 감정 분석 결과 조회 (최신 1건)
     */
    Optional<CallEmotion> findTopByCallRecordIdOrderByCreatedAtDesc(Long callRecordId);

    /**
     * 통화 ID로 모든 감정 분석 결과 조회
     */
    List<CallEmotion> findByCallRecordIdOrderByCreatedAtAsc(Long callRecordId);

    /**
     * 어르신의 최근 감정 상태 통계
     */
    @Query("SELECT ce.emotionLevel, COUNT(ce) FROM CallEmotion ce " +
            "JOIN ce.callRecord cr " +
            "WHERE cr.elderly.id = :elderlyId " +
            "AND ce.createdAt >= :startDate " +
            "GROUP BY ce.emotionLevel")
    List<Object[]> getEmotionStatsByElderly(@Param("elderlyId") Long elderlyId,
                                            @Param("startDate") LocalDateTime startDate);

    /**
     * 특정 감정 레벨의 통화 건수
     */
    long countByEmotionLevel(EmotionLevel emotionLevel);
}