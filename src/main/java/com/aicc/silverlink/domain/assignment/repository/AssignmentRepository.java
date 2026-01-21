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


    boolean existsByElderly_IdAndStatus(Long elderlyId, AssignmentStatus status);


    @Query("SELECT a FROM Assignment a " +
            "WHERE a.counselor.id = :counselorId " +
            "AND a.elderly.id = :elderlyId " +
            "AND a.status = :status")
    Optional<Assignment> findByCounselorAndElderlyAndStatus(
            @Param("counselorId") Long counselorId,
            @Param("elderlyId") Long elderlyId,
            @Param("status") AssignmentStatus status
    );


    @Query("SELECT a FROM Assignment a " +
            "JOIN FETCH a.elderly e " +   // 어르신 정보 한방에 로딩
            "JOIN FETCH e.user " +        // 어르신 이름/전화번호도 한방에
            "WHERE a.counselor.id = :counselorId " +
            "AND a.status = 'ACTIVE'")
    List<Assignment> findAllActiveByCounselorId(@Param("counselorId") Long counselorId);


    @Query("SELECT a FROM Assignment a " +
            "JOIN FETCH a.counselor c " + // 상담사 정보 한방에 로딩
            "JOIN FETCH c.user " +        // 상담사 이름/전화번호도 한방에
            "WHERE a.elderly.id = :elderlyId " +
            "AND a.status = 'ACTIVE'")
    Optional<Assignment> findActiveByElderlyId(@Param("elderlyId") Long elderlyId);
}