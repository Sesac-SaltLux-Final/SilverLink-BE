package com.aicc.silverlink.domain.call.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Python 챗봇 서비스로 전달되는 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotRequest {
    /**
     * 사용자 질문
     */
    private String message;

    /**
     * Thread ID (대화 세션 식별자)
     */
    private String threadId;

    /**
     * 보호자 ID (권한 필터링용)
     */
    private Long guardianId;

    /**
     * 어르신 ID (권한 필터링용)
     */
    private Long elderlyId;
}
