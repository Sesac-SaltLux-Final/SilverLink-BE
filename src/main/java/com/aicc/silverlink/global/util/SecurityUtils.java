package com.aicc.silverlink.global.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    private SecurityUtils() {}

    // 현재 로그인한 사용자의 ID(PK)를 가져오는 메소드
    public static Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) throw new IllegalStateException("NO_AUTH");

        return Long.valueOf(auth.getName());
    }
}