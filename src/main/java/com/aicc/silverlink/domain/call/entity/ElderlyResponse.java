package com.aicc.silverlink.domain.call.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 어르신에게 제공된 AI 응답
 */
@Entity
@Table(name = "elderly_responses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ElderlyResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "response_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    private LlmModel llmModel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "call_id", nullable = false)
    private CallRecord callRecord;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "responded_at", nullable = false)
    private LocalDateTime respondedAt;

    @Column(name = "danger", nullable = false)
    private boolean danger;

    @Column(name = "danger_reason", columnDefinition = "TEXT")
    private String dangerReason;

    @PrePersist
    protected void onCreate() {
        if (this.respondedAt == null) {
            this.respondedAt = LocalDateTime.now();
        }
    }
}