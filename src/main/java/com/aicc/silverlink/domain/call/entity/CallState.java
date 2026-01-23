package com.aicc.silverlink.domain.call.entity;

/**
 * 통화 상태
 */
public enum CallState {
    REQUESTED,   // 통화 요청됨
    ANSWERED,    // 통화 연결됨
    FAILED,      // 통화 실패
    COMPLETED,   // 통화 완료
    CANCELLED;   // 통화 취소

    /**
     * 한글 상태명 반환
     */
    public String getKorean() {
        return switch (this) {
            case REQUESTED -> "요청됨";
            case ANSWERED -> "연결됨";
            case FAILED -> "실패";
            case COMPLETED -> "완료";
            case CANCELLED -> "취소";
        };
    }
}