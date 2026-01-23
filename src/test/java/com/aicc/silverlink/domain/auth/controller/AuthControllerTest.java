package com.aicc.silverlink.domain.auth.controller;

import com.aicc.silverlink.domain.auth.dto.AuthDtos;
import com.aicc.silverlink.domain.auth.service.AuthService;
import com.aicc.silverlink.global.config.auth.AuthPolicyProperties;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
class AuthControllerIT {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        /**
         * ✅ 통합 컨텍스트는 다 올리되, 컨트롤러 테스트 목적이면 Service는 Mock이 가장 안정적
         * (DB/Redis/JWT/세션 발급 같은 외부 의존으로 빠지지 않게)
         */
        @MockitoBean
        private AuthService authService;

        @MockitoBean
        private AuthPolicyProperties props;

        @BeforeEach
        void setup() {
                // 기본 프로퍼티 Mocking
                given(props.getRefreshCookieName()).willReturn("refresh_token");
                given(props.getRefreshCookiePath()).willReturn("/");
                given(props.getRefreshCookieSameSite()).willReturn("Strict");
                given(props.getRefreshTtlSeconds()).willReturn(3600L);
                given(props.getAccessTtlSeconds()).willReturn(1800L);
        }

        @Test
        @DisplayName("로그인 성공 - 액세스 토큰 반환 및 리프레시 토큰 쿠키 설정")
        void login_Success() throws Exception {
                // given
                AuthDtos.LoginRequest request = new AuthDtos.LoginRequest("testUser", "password123!");

                AuthService.AuthResult authResult = new AuthService.AuthResult(
                                "access-token-sample",
                                "refresh-token-sample",
                                "session-id-123",
                                1800L);

                given(authService.login(any(AuthDtos.LoginRequest.class))).willReturn(authResult);

                // when & then
                mockMvc.perform(post("/api/auth/login")
                                .with(csrf()) // ✅ Spring Security 켜져 있으면 POST는 기본적으로 CSRF 필요할 때 많음
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").value("access-token-sample"))
                                .andExpect(jsonPath("$.expiresInSeconds").value(1800L))
                                // ⚠️ 실제 응답 role 값에 맞게 수정 필요 (너 프로젝트 role이 ADMIN/COUNSELOR/... 일 수 있음)
                                .andExpect(jsonPath("$.role").exists())
                                .andExpect(cookie().exists("refresh_token"))
                                .andExpect(cookie().value("refresh_token", "session-id-123.refresh-token-sample"))
                                .andExpect(cookie().httpOnly("refresh_token", true))
                                .andExpect(cookie().secure("refresh_token", true));
        }

        @Test
        @DisplayName("로그인 실패 - 비밀번호 불일치 (예외 처리 정책에 따라 상태코드 확인)")
        void login_Fail_InvalidPassword() throws Exception {
                // given
                AuthDtos.LoginRequest request = new AuthDtos.LoginRequest("testUser", "wrongPassword");

                given(authService.login(any(AuthDtos.LoginRequest.class)))
                                .willThrow(new IllegalArgumentException("LOGIN_FAIL"));

                // when & then
                mockMvc.perform(post("/api/auth/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andDo(print())
                                // ⚠️ 네 GlobalExceptionHandler 정책에 맞게 조정 (400/401/500 등)
                                .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("토큰 갱신 성공")
        void refresh_Success() throws Exception {
                // given
                String oldCookieValue = "session-id-123.old-refresh-token";
                Cookie cookie = new Cookie("refresh_token", oldCookieValue);

                AuthService.AuthResult authResult = new AuthService.AuthResult(
                                "new-access-token",
                                "new-refresh-token",
                                "session-id-123",
                                1800L);

                given(authService.refresh(eq("session-id-123"), eq("old-refresh-token")))
                                .willReturn(authResult);

                // when & then
                mockMvc.perform(post("/api/auth/refresh")
                                .with(csrf())
                                .cookie(cookie))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                                .andExpect(cookie().value("refresh_token", "session-id-123.new-refresh-token"));
        }

        @Test
        @DisplayName("로그아웃 성공 - 쿠키 삭제 확인")
        void logout_Success() throws Exception {
                // given
                String cookieValue = "session-id-123.refresh-token";
                Cookie cookie = new Cookie("refresh_token", cookieValue);

                // when & then
                mockMvc.perform(post("/api/auth/logout")
                                .with(csrf())
                                .cookie(cookie))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(cookie().maxAge("refresh_token", 0));

                verify(authService).logout("session-id-123");
        }
}
