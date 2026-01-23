package com.aicc.silverlink.domain.guardian.repository;

import com.aicc.silverlink.domain.guardian.entity.GuardianElderly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GuardianElderlyRepository extends JpaRepository<GuardianElderly, Long> {

    boolean existsByElderly_Id(Long elderlyId);

    boolean existsByGuardian_Id(Long guardianId);

    /**
     * 보호자 ID로 관계 정보 조회 (Fetch Join으로 성능 최적화)
     */
    @Query("SELECT ge FROM GuardianElderly ge " +
            "JOIN FETCH ge.elderly e " +
            "JOIN FETCH e.user " +
            "WHERE ge.guardian.id = :guardianId")
    Optional<GuardianElderly> findByGuardianId(@Param("guardianId") Long guardianId);

    /**
     * 어르신 ID로 관계 정보 조회 (Fetch Join으로 성능 최적화)
     */
    @Query("SELECT ge FROM GuardianElderly ge " +
            "JOIN FETCH ge.guardian g " +
            "JOIN FETCH g.user " +
            "WHERE ge.elderly.id = :elderlyId")
    Optional<GuardianElderly> findByElderlyId(@Param("elderlyId") Long elderlyId);

    /**
     * 보호자-어르신 관계 존재 여부 확인
     */
    boolean existsByGuardian_IdAndElderly_Id(Long guardianId, Long elderlyId);

    /**
     * ✅ 회원 탈퇴 시 호출: 보호자 기준 관계 데이터 삭제
     */
    @Modifying
    @Query("DELETE FROM GuardianElderly ge WHERE ge.guardian.id = :guardianId")
    void deleteByGuardianId(@Param("guardianId") Long guardianId);

    /**
     * ✅ 회원 탈퇴 시 호출: 어르신 기준 관계 데이터 삭제
     */
    @Modifying
    @Query("DELETE FROM GuardianElderly ge WHERE ge.elderly.id = :elderlyId")
    void deleteByElderlyId(@Param("elderlyId") Long elderlyId);
}
