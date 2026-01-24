package com.aicc.silverlink.domain.elderly.entity;

import com.aicc.silverlink.domain.system.entity.AdministrativeDivision;
import com.aicc.silverlink.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

@Entity
@Table(name = "elderly")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Elderly {

    @Id
    @Column(name = "user_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    // 담당 행정 구역 - AdministrativeDivision과 FK 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adm_code", nullable = false)
    private AdministrativeDivision administrativeDivision;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false)
    private Gender gender;

    @Column(name = "address_line1", length = 200)
    private String addressLine1;

    @Column(name = "address_line2", length = 200)
    private String addressLine2;

    @Column(name = "zipcode", length = 10)
    private String zipcode;


    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum Gender {
        M, F
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 편의 메서드: 행정구역 코드 반환
     */
    public Long getAdmCode() {
        return administrativeDivision != null ? administrativeDivision.getAdmCode() : null;
    }

    private Elderly(User user, AdministrativeDivision administrativeDivision, LocalDate birthDate, Gender gender) {
        if (user == null)
            throw new IllegalArgumentException("USER_REQUIRED");
        if (administrativeDivision == null)
            throw new IllegalArgumentException("ADM_DIVISION_REQUIRED");
        if (birthDate == null)
            throw new IllegalArgumentException("BIRTH_REQUIRED");
        if (gender == null)
            throw new IllegalArgumentException("GENDER_REQUIRED");

        this.user = user;
        this.administrativeDivision = administrativeDivision;
        this.birthDate = birthDate;
        this.gender = gender;
        // DB 스키마 호환성을 위해 adm_dong_code 설정 (adm_code와 동일하게)

    }

    public static Elderly create(User user, AdministrativeDivision administrativeDivision,
            LocalDate birthDate, Gender gender) {
        return new Elderly(user, administrativeDivision, birthDate, gender);
    }

    public void updateAddress(String line1, String line2, String zipcode) {
        this.addressLine1 = normalize(line1, 200);
        this.addressLine2 = normalize(line2, 200);
        this.zipcode = normalize(zipcode, 10);
    }

    public void changeAdministrativeDivision(AdministrativeDivision administrativeDivision) {
        if (administrativeDivision == null)
            throw new IllegalArgumentException("ADM_DIVISION_REQUIRED");
        this.administrativeDivision = administrativeDivision;
    }

    public int age() {
        return Period.between(this.birthDate, LocalDate.now()).getYears();
    }

    private String normalize(String v, int max) {
        if (v == null)
            return null;
        String t = v.trim();
        if (t.isEmpty())
            return null;
        if (t.length() > max)
            throw new IllegalArgumentException("FIELD_TOO_LONG");
        return t;
    }
}