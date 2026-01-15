package com.aicc.silverlink.domain.consent.entity;

import com.aicc.silverlink.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_consents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserConsent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "consent_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false)
    private ConsentType consentType;

    @Column(name = "is_agreed", nullable = false)
    private boolean isAgreed;

    @Column(name = "agreed_at", nullable = false)
    private LocalDateTime agreedAt;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    @PrePersist
    protected void onCreate() {
        if (this.agreedAt == null) {
            this.agreedAt = LocalDateTime.now();
        }
    }

    public enum ConsentType {
        PRIVACY, SENSITIVE_INFO, THIRD_PARTY, MEDICATION
    }
}
