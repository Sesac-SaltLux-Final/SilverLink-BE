package com.aicc.silverlink.domain.call.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 통화 중 감정 분석 결과
 */
@Entity
@Table(name = "call_emotions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CallEmotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "emotion_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "call_id", nullable = false)
    private CallRecord callRecord;

    @Enumerated(EnumType.STRING)
    @Column(name = "emotion_level", nullable = false)
    private EmotionLevel emotionLevel;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.emotionLevel == null) {
            this.emotionLevel = EmotionLevel.NORMAL;
        }
    }

    /**
     * 감정 레벨을 한글로 반환
     */
    public String getEmotionLevelKorean() {
        return emotionLevel.getKorean();
    }
}