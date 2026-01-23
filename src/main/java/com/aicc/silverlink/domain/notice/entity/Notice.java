package com.aicc.silverlink.domain.notice.entity;

import com.aicc.silverlink.domain.admin.entity.Admin;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notices")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Notice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notice_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_admin_user_id", nullable = false)
    private Admin createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private NoticeCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_mode", nullable = false)
    private TargetMode targetMode;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private NoticeStatus status;

    @Column(name = "is_priority", nullable = false)
    private boolean isPriority;

    @Column(name = "is_popup", nullable = false)
    private boolean isPopup;

    @Column(name = "popup_start_at")
    private LocalDateTime popupStartAt;

    @Column(name = "popup_end_at")
    private LocalDateTime popupEndAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.targetMode == null) {
            this.targetMode = TargetMode.ALL;
        }
        if (this.status == null) {
            this.status = NoticeStatus.DRAFT;
        }
        if (this.category == null) {
            this.category = NoticeCategory.NOTICE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum TargetMode {
        ALL, ROLE_SET
    }

    public enum NoticeStatus {
        DRAFT, PUBLISHED, ARCHIVED, DELETED
    }
}
