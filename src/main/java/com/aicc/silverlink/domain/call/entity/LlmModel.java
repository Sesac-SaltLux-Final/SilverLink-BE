package com.aicc.silverlink.domain.call.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * LLM 모델 실행 단위 (프롬프트)
 * DDL: llm_models 테이블
 */
@Entity
@Table(name = "llm_models")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class LlmModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "model_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "call_id", nullable = false)
    private CallRecord callRecord;

    @Column(name = "prompt", columnDefinition = "TEXT", nullable = false)
    private String prompt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "llmModel", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ElderlyResponse> responses = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}