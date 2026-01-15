package com.aicc.silverlink.domain.welfare.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "welfare_services")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WelfareService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "welfare_service_id")
    private Long id;

    @Column(name = "serv_id", nullable = false, length = 50)
    private String servId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private Source source;

    @Column(name = "serv_nm", length = 255)
    private String servNm;

    @Column(name = "jur_mnof_nm", length = 255)
    private String jurMnofNm;

    @Column(name = "serv_dgst", columnDefinition = "TEXT")
    private String servDgst;

    @Column(name = "target_dtl_cn", columnDefinition = "TEXT")
    private String targetDtlCn;

    @Column(name = "slct_crit_cn", columnDefinition = "TEXT")
    private String slctCritCn;

    @Column(name = "alw_serv_cn", columnDefinition = "TEXT")
    private String alwServCn;

    @Column(name = "rprs_ctadr", length = 100)
    private String rprsCtadr;

    @Column(name = "serv_dtl_link", columnDefinition = "TEXT")
    private String servDtlLink;

    @Column(name = "district_code", length = 20)
    private String districtCode;

    @Column(name = "category", length = 50)
    private String category;

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

    public enum Source {
        CENTRAL, LOCAL
    }
}
