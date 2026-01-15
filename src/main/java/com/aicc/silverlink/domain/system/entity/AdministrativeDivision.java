package com.aicc.silverlink.domain.system.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "administrative_division")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdministrativeDivision {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "adm_code")
    private Long admCode;

    @Column(name = "sido_code", nullable = false, length = 2)
    private String sidoCode;

    @Column(name = "sigungu_code", length = 3)
    private String sigunguCode;

    @Column(name = "dong_code", length = 3)
    private String dongCode;

    @Column(name = "sido_name", nullable = false, length = 20)
    private String sidoName;

    @Column(name = "sigungu_name", length = 20)
    private String sigunguName;

    @Column(name = "dong_name", length = 20)
    private String dongName;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false)
    private Level level;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.isActive == false) { // Default true logic handled elsewhere or assume true by default if needed
             this.isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum Level {
        SIDO, SIGUNGU, DONG
    }
}
