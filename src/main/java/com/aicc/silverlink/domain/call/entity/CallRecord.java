package com.aicc.silverlink.domain.call.entity;

import com.aicc.silverlink.domain.elderly.entity.Elderly;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AI CallBot과 어르신 간의 통화 기록
 */
@Entity
@Table(name = "call_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CallRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "call_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "elderly_user_id", nullable = false)
    private Elderly elderly;

    @Column(name = "call_at", nullable = false)
    private LocalDateTime callAt;

    @Column(name = "call_time_sec", nullable = false)
    private Integer callTimeSec;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private CallState state;

    @OneToMany(mappedBy = "callRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ElderlyResponse> elderlyResponses = new ArrayList<>();

    @OneToMany(mappedBy = "callRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CallSummary> summaries = new ArrayList<>();

    @OneToMany(mappedBy = "callRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CallEmotion> emotions = new ArrayList<>();

    @OneToMany(mappedBy = "callRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CounselorCallReview> reviews = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.callAt == null) {
            this.callAt = LocalDateTime.now();
        }
        if (this.state == null) {
            this.state = CallState.REQUESTED;
        }
    }

    /**
     * 통화 시간(초)을 "분:초" 형식으로 반환
     */
    public String getFormattedDuration() {
        if (callTimeSec == null || callTimeSec == 0) {
            return "0:00";
        }
        int minutes = callTimeSec / 60;
        int seconds = callTimeSec % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    /**
     * 위험 응답이 있는지 확인
     */
    public boolean hasDangerResponse() {
        return elderlyResponses.stream().anyMatch(ElderlyResponse::isDanger);
    }
}