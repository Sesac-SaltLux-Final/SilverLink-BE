package com.aicc.silverlink.domain.counseling.entity;

import com.aicc.silverlink.domain.call.entity.CallLog;
import com.aicc.silverlink.domain.counselor.entity.Counselor;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "counseling_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CounselingRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "counseling_record_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "call_log_id", nullable = false)
    private CallLog callLog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counselor_user_id")
    private Counselor counselor;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
