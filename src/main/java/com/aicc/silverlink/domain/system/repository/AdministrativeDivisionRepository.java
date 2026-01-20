package com.aicc.silverlink.domain.system.repository;

import com.aicc.silverlink.domain.system.entity.AdministrativeDivision;
import com.aicc.silverlink.domain.system.entity.AdministrativeDivision.DivisionLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdministrativeDivisionRepository extends JpaRepository<AdministrativeDivision, Long> {

    /**
     * 시/도 목록 조회
     */
    @Query("SELECT a FROM AdministrativeDivision a WHERE a.level = 'SIDO' AND a.isActive = true ORDER BY a.sidoCode")
    List<AdministrativeDivision> findAllSido();

    /**
     * 특정 시/도의 시/군/구 목록
     */
    @Query("""
        SELECT a FROM AdministrativeDivision a
        WHERE a.level = 'SIGUNGU'
        AND a.sidoCode = :sidoCode
        AND a.isActive = true
        ORDER BY a.sigunguCode
        """)
    List<AdministrativeDivision> findSigunguBySido(@Param("sidoCode") String sidoCode);

    /**
     * 특정 시/군/구의 읍/면/동 목록
     */
    @Query("""
        SELECT a FROM AdministrativeDivision a
        WHERE a.level = 'DONG'
        AND a.sidoCode = :sidoCode
        AND a.sigunguCode = :sigunguCode
        AND a.isActive = true
        ORDER BY a.dongCode
        """)
    List<AdministrativeDivision> findDongBySigungu(
            @Param("sidoCode") String sidoCode,
            @Param("sigunguCode") String sigunguCode
    );
}