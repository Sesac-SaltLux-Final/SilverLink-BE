package com.aicc.silverlink.global.util;

import com.aicc.silverlink.global.security.principal.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    private SecurityUtils() {
    }

    // 현재 로그인한 사용자의 ID(PK)를 가져오는 메소드
    public static Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            throw new IllegalStateException("인증되지 않은 사용자입니다.");
        }

        Object principal = auth.getPrincipal();

        // UserPrincipal 타입 처리
        if (principal instanceof UserPrincipal) {
            return ((UserPrincipal) principal).getUserId();
        }

        // Long 타입 처리 (하위 호환성)
        if (principal instanceof Long) {
            return (Long) principal;
        }

        throw new IllegalStateException("알 수 없는 Principal 타입: " + principal.getClass().getName());
    }

    // 인증 여부 확인
    public static boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && auth.getPrincipal() != null;
    }
}