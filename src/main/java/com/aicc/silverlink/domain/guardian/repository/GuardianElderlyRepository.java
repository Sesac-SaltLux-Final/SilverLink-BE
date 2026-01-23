package com.aicc.silverlink.domain.guardian.repository;

import com.aicc.silverlink.domain.guardian.entity.GuardianElderly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GuardianElderlyRepository extends JpaRepository<GuardianElderly, Long> {
    boolean existsByElderly_Id(Long elderlyId);

    @Query("SELECT ge FROM GuardianElderly ge " +
            "JOIN FETCH ge.elderly e " +
            "JOIN FETCH e.user " + // 어르신 이름도 알아야 하니까
            "WHERE ge.guardian.id = :guardianId")
    Optional<GuardianElderly> findByGuardianId(@Param("guardianId") Long guardianId);

    @Query("SELECT ge FROM GuardianElderly ge " +
            "JOIN FETCH ge.guardian g " +
            "JOIN FETCH g.user " + // 보호자 이름도 알아야 하니까
            "WHERE ge.elderly.id = :elderlyId")
    Optional<GuardianElderly> findByElderlyId(@Param("elderlyId") Long elderlyID);

    /**
     * 보호자-어르신 관계 존재 여부 확인 (챗봇 권한 검증용)
     */
    boolean existsByGuardian_IdAndElderly_Id(Long guardianId, Long elderlyId);
}
