package com.aicc.silverlink.domain.policy.entity;

import com.aicc.silverlink.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "policies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Policy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_id")
    private Long id;

    @Column(name = "policy_key", nullable = false, unique = true, length = 100)
    private String policyKey;

    @Column(name = "policy_value", nullable = false, length = 255)
    private String policyValue;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
