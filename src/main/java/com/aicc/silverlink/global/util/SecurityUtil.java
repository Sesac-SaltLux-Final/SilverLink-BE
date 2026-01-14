package com.aicc.silverlink.global.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {
    // 현재 로그인한 사용자의 ID(PK)를 가져오는 메소드
    public static Long getCurrentUserId() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Security Context에 인증 정보가 없습니다.");
        }
        // Principal에 저장된 유저 ID를 반환 (보통 String이나 Long)
        return Long.parseLong(authentication.getName());
    }
}