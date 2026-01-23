package com.aicc.silverlink.domain.auth.controller;

import com.aicc.silverlink.domain.auth.dto.AuthDtos;
import com.aicc.silverlink.domain.auth.service.AuthService;
import com.aicc.silverlink.global.config.auth.AuthPolicyProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "인증", description = "로그인/로그아웃/토큰 갱신 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthPolicyProperties props;

    @PostMapping("/login")
    public AuthDtos.TokenResponse login(@RequestBody AuthDtos.LoginRequest req, HttpServletResponse res) {

        AuthService.AuthResult result = authService.login(req);

        String cookieValue = result.sid() + "." + result.refreshToken();
        setRefreshCookie(res, cookieValue);

        return new AuthDtos.TokenResponse(result.accessToken(), result.ttl(), "USER");
    }

    @PostMapping("/refresh")
    public AuthDtos.RefreshResponse refresh(HttpServletRequest req, HttpServletResponse res) {
        String cookieValue = readCookie(req, props.getRefreshCookieName());

        if (cookieValue == null || !cookieValue.contains(".")) {
            throw new IllegalArgumentException("NO_REFRESH_TOKEN");
        }

        String sid = cookieValue.substring(0, cookieValue.indexOf('.'));
        String refreshToken = cookieValue.substring(cookieValue.indexOf('.') + 1);

        AuthService.AuthResult result = authService.refresh(sid, refreshToken);

        String newCookieValue = result.sid() + "." + result.refreshToken();
        setRefreshCookie(res, newCookieValue);

        return new AuthDtos.RefreshResponse(result.accessToken(), result.ttl());

    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest req, HttpServletResponse res) {
        String cookieVal = readCookie(req, props.getRefreshCookieName());
        if (cookieVal != null && cookieVal.contains(".")) {
            String sid = cookieVal.substring(0, cookieVal.indexOf('.'));
            authService.logout(sid);

            ResponseCookie clear = ResponseCookie.from(props.getRefreshCookieName(), "")
                    .httpOnly(true)
                    .secure(true)
                    .path(props.getRefreshCookiePath())
                    .maxAge(0)
                    .sameSite(props.getRefreshCookieSameSite())
                    .build();

            res.addHeader("Set-Cookie", clear.toString());
        }
    }

    private void setRefreshCookie(HttpServletResponse res, String value) {
        ResponseCookie cookie = ResponseCookie.from(props.getRefreshCookieName(), value)
                .httpOnly(true)
                .secure(true)
                .path(props.getRefreshCookiePath())
                .maxAge(props.getRefreshTtlSeconds())
                .sameSite(props.getRefreshCookieSameSite())
                .build();
        res.addHeader("Set-Cookie", cookie.toString());
    }

    private String readCookie(HttpServletRequest req, String name) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null)
            return null;
        for (Cookie c : cookies) {
            if (name.equals(c.getName()))
                return c.getValue();
        }

        return null;
    }
}
