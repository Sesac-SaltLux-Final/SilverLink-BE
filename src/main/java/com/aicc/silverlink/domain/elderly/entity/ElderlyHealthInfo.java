package com.aicc.silverlink.domain.elderly.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "elderly_health_info")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ElderlyHealthInfo {
    @Id
    @Column(name = "elderly_user_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "elderly_user_id")
    private Elderly elderly;

    @Column(name = "chronic_diseases", columnDefinition = "TEXT")
    private String chronicDiseases;

    @Column(name = "mental_health_notes", columnDefinition = "TEXT")
    private String mentalHealthNotes;

    @Column(name = "special_notes", columnDefinition = "TEXT")
    private String specialNotes;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
