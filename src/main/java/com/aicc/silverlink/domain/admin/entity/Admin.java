package com.aicc.silverlink.domain.admin.entity;

import com.aicc.silverlink.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자 엔티티
 * User 테이블과 1:1 관계
 */
@Entity
@Table(name = "admin")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Admin {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    // 담당 행정 구역 코드
    @Column(name = "adm_dong_code", nullable = false)
    private Long admDongCode;

    // 관리자 레벨
    @Enumerated(EnumType.STRING)
    @Column(name = "admin_level", nullable = false)
    private AdminLevel adminLevel;

    @Builder
    public Admin(User user, Long admDongCode, AdminLevel adminLevel) {
        this.user = user;
        this.admDongCode = admDongCode;
        this.adminLevel = adminLevel != null ? adminLevel : determineAdminLevel(admDongCode);
    }

    /**
     * 행정동 코드로 관리자 레벨 자동 결정
     *
     * 행정동 코드 체계:
     * - 시/도: 11XXXXXXXX (2자리 + 8자리 0)
     * - 시/군/구: 1168XXXXXX (4자리 + 6자리 0)
     * - 읍/면/동: 1168010XXX (7자리 + 3자리)
     * - 리: 1168010100 (10자리 전체)
     */
    private static AdminLevel determineAdminLevel(Long admDongCode) {
        if (admDongCode == null) {
            return AdminLevel.DISTRICT;
        }

        String code = String.format("%010d", admDongCode);

        // 전국 관리자 (00 0000 000 00)
        if (admDongCode == 0L) {
            return AdminLevel.NATIONAL;
        }

        // 시/도 레벨 (XX 0000 000 00)
        if (code.substring(2).equals("00000000")) {
            return AdminLevel.PROVINCIAL;
        }

        // 시/군/구 레벨 (XX XX00 000 00)
        if (code.substring(4).equals("000000")) {
            return AdminLevel.CITY;
        }

        // 읍/면/동 레벨
        return AdminLevel.DISTRICT;
    }

    /**
     * 관리자 레벨 Enum
     */
    public enum AdminLevel {
        NATIONAL("전국", 0),      // 중앙 관리자
        PROVINCIAL("시/도", 2),   // 서울특별시, 경기도 등
        CITY("시/군/구", 4),      // 강남구, 수원시 등
        DISTRICT("읍/면/동", 7);  // 역삼동 등

        private final String description;
        private final int codeLength;  // 유효한 코드 길이

        AdminLevel(String description, int codeLength) {
            this.description = description;
            this.codeLength = codeLength;
        }

        public String getDescription() {
            return description;
        }

        public int getCodeLength() {
            return codeLength;
        }
    }

    /**
     * 비즈니스 로직: 담당 구역 변경
     */
    public void updateAdmDongCode(Long newAdmDongCode) {
        this.admDongCode = newAdmDongCode;
        this.adminLevel = determineAdminLevel(newAdmDongCode);
    }

    /**
     * 특정 행정구역이 이 관리자의 관할 구역인지 확인
     */
    public boolean hasJurisdiction(Long targetAdmDongCode) {
        if (targetAdmDongCode == null) {
            return false;
        }

        // 전국 관리자는 모든 구역 관할
        if (this.adminLevel == AdminLevel.NATIONAL) {
            return true;
        }

        String myCode = String.format("%010d", this.admDongCode);
        String targetCode = String.format("%010d", targetAdmDongCode);

        // 내 코드 길이만큼 비교
        int compareLength = this.adminLevel.getCodeLength();
        if (compareLength == 0) {
            return true;
        }

        return myCode.substring(0, compareLength)
                .equals(targetCode.substring(0, compareLength));
    }

    /**
     * 다른 관리자가 내 상위 관리자인지 확인
     */
    public boolean isSubordinateOf(Admin other) {
        return other.hasJurisdiction(this.admDongCode)
                && this.adminLevel.ordinal() > other.adminLevel.ordinal();
    }

    /**
     * 다른 관리자보다 상위 레벨인지 확인
     */
    public boolean hasHigherLevelThan(Admin other) {
        return this.adminLevel.ordinal() < other.adminLevel.ordinal();
    }
}