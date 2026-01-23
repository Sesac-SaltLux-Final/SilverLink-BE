package com.aicc.silverlink.domain.call.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "llm_models")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LlmModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "model_id")
    private Long id;

    // TODO: Add fields mapping to llm_models table columns
    // e.g., model_name, version, provider, etc.

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
