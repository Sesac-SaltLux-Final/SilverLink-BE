package com.aicc.silverlink.domain.elderly.entity;

import com.aicc.silverlink.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

@Entity
@Table(name = "elderly")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Elderly {

    @Id
    @Column(name = "user_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "adm_dong_code", nullable = false, length = 20)
    private String admDongCode;

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

    public enum Gender { M, F }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    private Elderly(User user, String admDongCode, LocalDate birthDate, Gender gender) {
        if (user == null) throw new IllegalArgumentException("USER_REQUIRED");
        if (admDongCode == null || admDongCode.isBlank()) throw new IllegalArgumentException("ADM_DONG_REQUIRED");
        if (birthDate == null) throw new IllegalArgumentException("BIRTH_REQUIRED");
        if (gender == null) throw new IllegalArgumentException("GENDER_REQUIRED");

        this.user = user;
        this.admDongCode = admDongCode;
        this.birthDate = birthDate;
        this.gender = gender;
    }

    public static Elderly create(User user, String admDongCode, LocalDate birthDate, Gender gender) {
        return new Elderly(user, admDongCode, birthDate, gender);
    }

    public void updateAddress(String line1, String line2, String zipcode) {
        this.addressLine1 = normalize(line1, 200);
        this.addressLine2 = normalize(line2, 200);
        this.zipcode = normalize(zipcode, 10);
    }

    public void changeAdmDongCode(String admDongCode) {
        if (admDongCode == null || admDongCode.isBlank()) throw new IllegalArgumentException("ADM_DONG_REQUIRED");
        this.admDongCode = admDongCode;
    }

    public int age() {
        return Period.between(this.birthDate, LocalDate.now()).getYears();
    }

    private String normalize(String v, int max) {
        if (v == null) return null;
        String t = v.trim();
        if (t.isEmpty()) return null;
        if (t.length() > max) throw new IllegalArgumentException("FIELD_TOO_LONG");
        return t;
    }
}
