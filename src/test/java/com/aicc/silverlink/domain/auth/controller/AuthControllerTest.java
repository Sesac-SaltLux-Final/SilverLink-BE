package com.aicc.silverlink.domain.auth.controller;

import com.aicc.silverlink.domain.auth.dto.AuthDtos;
import com.aicc.silverlink.domain.auth.service.AuthService;
import com.aicc.silverlink.domain.session.service.SessionService;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.global.config.auth.AuthPolicyProperties;
import com.aicc.silverlink.global.security.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
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

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
class AuthControllerTest {

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
        private SessionService sessionService;

        @MockitoBean
        private JwtTokenProvider jwtTokenProvider;

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
                given(props.getIdleTtlSeconds()).willReturn(3600L);

                // ⭐ 추가: 이 부분이 누락되어 105번 줄에서 에러가 났던 것입니다.
                given(props.getRefreshCookieSecure()).willReturn(true);
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
                                1800L,
                                Role.ADMIN,
                                1L);

                given(authService.login(any(AuthDtos.LoginRequest.class))).willReturn(authResult);

                // when & then
                mockMvc.perform(post("/api/auth/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andDo(print())
                                .andExpect(status().isOk())
                                // ⚠️ 주의: DTO 필드명이 만약 snake_case로 바뀌었다면 $.access_token으로 수정해야 함
                                .andExpect(jsonPath("$.accessToken").value("access-token-sample"))
                                .andExpect(jsonPath("$.expiresInSeconds").value(1800L))
                                .andExpect(jsonPath("$.role").value("ADMIN")) // exists()보다 구체적으로 검증
                                .andExpect(cookie().exists("refresh_token"))
                                .andExpect(cookie().value("refresh_token", "session-id-123.refresh-token-sample"))
                                .andExpect(cookie().httpOnly("refresh_token", true))
                                .andExpect(cookie().secure("refresh_token", true)); // 이제 true가 정상 반환됩니다!
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
                                // 401 Unauthorized Expectation
                                .andExpect(status().isUnauthorized());
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
                                1800L,
                                Role.ADMIN,
                                1L);

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

        @Test
        @DisplayName("세션 정보 조회 성공 - 남은 시간 반환")
        void getSessionInfo_Success() throws Exception {
                // given
                String token = "valid-jwt-token";
                String sid = "session-id-123";
                Long userId = 1L;
                long now = Instant.now().getEpochSecond();
                long lastSeen = now - 600; // 10분 전
                long idleTtl = 3600L; // 60분
                long expiresAt = lastSeen + idleTtl;

                // JWT 파싱 Mock - Jws와 Claims를 mock으로 생성
                @SuppressWarnings("unchecked")
                Jws<Claims> jws = mock(Jws.class);
                Claims claims = mock(Claims.class);

                given(jwtTokenProvider.parseAndValidate(token)).willReturn(jws);
                given(jws.getPayload()).willReturn(claims);
                given(jwtTokenProvider.getSid(any(Claims.class))).willReturn(sid);
                given(jwtTokenProvider.getUserId(any(Claims.class))).willReturn(userId);

                // 세션 메타데이터 Mock
                Map<String, String> sessionMeta = new HashMap<>();
                sessionMeta.put("userId", String.valueOf(userId));
                sessionMeta.put("role", "ADMIN");
                sessionMeta.put("lastSeen", String.valueOf(lastSeen));
                given(sessionService.getSessionMeta(sid)).willReturn(sessionMeta);

                // when & then
                mockMvc.perform(get("/api/auth/session/info")
                                .header("Authorization", "Bearer " + token))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.sid").value(sid))
                                .andExpect(jsonPath("$.lastSeen").value(lastSeen))
                                .andExpect(jsonPath("$.expiresAt").value(expiresAt))
                                .andExpect(jsonPath("$.remainingSeconds").exists())
                                .andExpect(jsonPath("$.idleTtl").value(idleTtl));
        }

        @Test
        @DisplayName("세션 정보 조회 실패 - 토큰 없음")
        void getSessionInfo_Fail_NoToken() throws Exception {
                // when & then
                mockMvc.perform(get("/api/auth/session/info"))
                                .andDo(print())
                                .andExpect(status().isBadRequest()); // 400
        }

        @Test
        @DisplayName("세션 정보 조회 실패 - 세션 만료")
        void getSessionInfo_Fail_SessionExpired() throws Exception {
                // given
                String token = "valid-jwt-token";
                String sid = "session-id-123";
                Long userId = 1L;

                // JWT 파싱 Mock
                @SuppressWarnings("unchecked")
                Jws<Claims> jws = mock(Jws.class);
                Claims claims = mock(Claims.class);

                given(jwtTokenProvider.parseAndValidate(token)).willReturn(jws);
                given(jws.getPayload()).willReturn(claims);
                given(jwtTokenProvider.getSid(any(Claims.class))).willReturn(sid);
                given(jwtTokenProvider.getUserId(any(Claims.class))).willReturn(userId);

                // 세션 만료 Mock
                given(sessionService.getSessionMeta(sid))
                                .willThrow(new IllegalStateException("SESSION_EXPIRED"));

                // when & then
                mockMvc.perform(get("/api/auth/session/info")
                                .header("Authorization", "Bearer " + token))
                                .andDo(print())
                                .andExpect(status().isUnauthorized()); // 401
        }
}
