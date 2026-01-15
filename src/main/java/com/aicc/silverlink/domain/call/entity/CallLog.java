package com.aicc.silverlink.domain.call.entity;

import com.aicc.silverlink.domain.counselor.entity.Counselor;
import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.domain.guardian.entity.Guardian;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "call_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CallLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "call_log_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "elderly_user_id", nullable = false)
    private Elderly elderly;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counselor_user_id")
    private Counselor counselor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guardian_user_id")
    private Guardian guardian;

    @Column(name = "provider", length = 30)
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    private Direction direction;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CallStatus status;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "summary_text", columnDefinition = "TEXT")
    private String summaryText;

    @Column(name = "analysis_json", columnDefinition = "JSON")
    private String analysisJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.direction == null) {
            this.direction = Direction.OUTBOUND;
        }
        if (this.status == null) {
            this.status = CallStatus.REQUESTED;
        }
    }

    public enum Direction {
        OUTBOUND, INBOUND
    }

    public enum CallStatus {
        REQUESTED, ANSWERED, FAILED, COMPLETED, CANCELLED
    }
}
