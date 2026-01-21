package com.aicc.silverlink.domain.admin.repository;

import com.aicc.silverlink.domain.admin.entity.Admin;
import com.aicc.silverlink.domain.admin.entity.Admin.AdminLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Admin Repository
 */
public interface AdminRepository extends JpaRepository<Admin, Long> {

    /**
     * User 정보를 포함하여 조회
     */
    @Query("SELECT a FROM Admin a JOIN FETCH a.user WHERE a.userId = :userId")
    Optional<Admin> findByIdWithUser(@Param("userId") Long userId);

    /**
     * 행정동 코드로 관리자 목록 조회
     */
    @Query("SELECT a FROM Admin a JOIN FETCH a.user WHERE a.admDongCode = :admDongCode")
    List<Admin> findByAdmDongCode(@Param("admDongCode") Long admDongCode);

    /**
     * 관리자 레벨로 조회
     */
    @Query("SELECT a FROM Admin a JOIN FETCH a.user WHERE a.adminLevel = :adminLevel")
    List<Admin> findByAdminLevel(@Param("adminLevel") AdminLevel adminLevel);

    /**
     * 모든 관리자 조회 (User 정보 포함)
     */
    @Query("SELECT a FROM Admin a JOIN FETCH a.user")
    List<Admin> findAllWithUser();

    /**
     * 특정 행정구역의 상위 관리자 찾기
     * 예: 역삼동(1168010100)의 상위 관리자 = 강남구(11680000) 관리자
     */
    @Query(value = """
        SELECT a.* FROM admin a
        JOIN users u ON a.user_id = u.user_id
        WHERE a.adm_dong_code IN (
            -- 시/도 레벨 (앞 2자리)
            CAST(SUBSTRING(LPAD(:targetCode, 10, '0'), 1, 2) * 100000000 AS UNSIGNED),
            -- 시/군/구 레벨 (앞 4자리)
            CAST(SUBSTRING(LPAD(:targetCode, 10, '0'), 1, 4) * 1000000 AS UNSIGNED),
            -- 읍/면/동 레벨 (앞 7자리)
            CAST(SUBSTRING(LPAD(:targetCode, 10, '0'), 1, 7) * 1000 AS UNSIGNED)
        )
        AND a.admin_level != 'DISTRICT'
        ORDER BY a.admin_level ASC
        """, nativeQuery = true)
    List<Admin> findSupervisors(@Param("targetCode") Long targetCode);

    /**
     * 특정 관리자의 하위 관리자들 조회
     * 예: 강남구 관리자의 하위 = 역삼동, 삼성동 등의 관리자들
     */
    @Query("""
        SELECT a FROM Admin a JOIN FETCH a.user
        WHERE a.adminLevel > :level
        AND FUNCTION('SUBSTRING', FUNCTION('LPAD', CAST(a.admDongCode AS string), 10, '0'), 1, :codeLength)
            = FUNCTION('SUBSTRING', FUNCTION('LPAD', CAST(:baseCode AS string), 10, '0'), 1, :codeLength)
        ORDER BY a.adminLevel ASC, a.admDongCode ASC
        """)
    List<Admin> findSubordinates(
            @Param("level") AdminLevel level,
            @Param("baseCode") Long baseCode,
            @Param("codeLength") int codeLength
    );

    /**
     * 관리자 존재 여부 확인
     */
    boolean existsByUserId(Long userId);
}