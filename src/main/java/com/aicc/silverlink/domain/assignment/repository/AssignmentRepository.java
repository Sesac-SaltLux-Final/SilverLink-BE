package com.aicc.silverlink.domain.assignment.repository;

import com.aicc.silverlink.domain.assignment.entity.Assignment;
import com.aicc.silverlink.domain.assignment.entity.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    // 1. 특정 어르신이 이미 배정된 상태인지 확인 (중복 배정 방지용)
    boolean existsByElderly_IdAndStatus(Long elderlyId, AssignmentStatus status);

    // 2. [핵심 권한 체크] 상담사가 특정 어르신을 담당하고 있는지 확인 (ACTIVE 상태 기준)
    // GuardianService의 validateAssignment 메서드에서 사용됩니다.
    boolean existsByCounselor_IdAndElderly_IdAndStatus(Long counselorId, Long elderlyId, AssignmentStatus status);

    // 3. 특정 배정 내역 조회 (해지/상태 변경용)
    @Query("SELECT a FROM Assignment a " +
            "WHERE a.counselor.id = :counselorId " +
            "AND a.elderly.id = :elderlyId " +
            "AND a.status = :status")
    Optional<Assignment> findByCounselorAndElderlyAndStatus(
            @Param("counselorId") Long counselorId,
            @Param("elderlyId") Long elderlyId,
            @Param("status") AssignmentStatus status
    );

    // 4. 상담사의 배정 현황 (목록 조회용 - FETCH JOIN으로 성능 최적화)
    @Query("SELECT a FROM Assignment a " +
            "JOIN FETCH a.elderly e " +
            "JOIN FETCH e.user " +
            "WHERE a.counselor.id = :counselorId " +
            "AND a.status = 'ACTIVE'")
    List<Assignment> findAllActiveByCounselorId(@Param("counselorId") Long counselorId);

    // 5. 어르신의 배정 현황 (단건 조회용 - FETCH JOIN으로 성능 최적화)
    @Query("SELECT a FROM Assignment a " +
            "JOIN FETCH a.counselor c " +
            "JOIN FETCH c.user " +
            "WHERE a.elderly.id = :elderlyId " +
            "AND a.status = 'ACTIVE'")
    Optional<Assignment> findActiveByElderlyId(@Param("elderlyId") Long elderlyId);
}