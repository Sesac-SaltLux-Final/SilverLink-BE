package com.aicc.silverlink.domain.call.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "elderly_responses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ElderlyResponse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "response_id")
    private Long id;

    // TODO: Add fields mapping to elderly_responses table columns
    // e.g., call_record_id, question_id, response_text, etc.

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
