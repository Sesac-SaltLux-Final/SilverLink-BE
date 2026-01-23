package com.aicc.silverlink.domain.call.entity;

/**
 * 통화 중 감지된 감정 수준
 */
public enum EmotionLevel {
    GOOD,       // 좋음
    NORMAL,     // 보통
    BAD,        // 나쁨
    DEPRESSED;  // 우울

    /**
     * 한글 감정명 반환
     */
    public String getKorean() {
        return switch (this) {
            case GOOD -> "좋음";
            case NORMAL -> "보통";
            case BAD -> "나쁨";
            case DEPRESSED -> "우울";
        };
    }
}