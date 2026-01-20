package com.aicc.silverlink.domain.auth.service;

import com.aicc.silverlink.domain.auth.dto.AuthDtos;
import com.aicc.silverlink.domain.session.service.SessionService;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import com.aicc.silverlink.global.config.auth.AuthPolicyProperties;
import com.aicc.silverlink.global.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

        @Mock
        private UserRepository userRepository;

        @Mock
        private PasswordEncoder passwordEncoder;

        @Mock
        private JwtTokenProvider jwt;

        @Mock
        private SessionService sessionService;

        @Mock
        private AuthPolicyProperties props;

        @Mock
        private StringRedisTemplate redis;

        @Mock
        private ValueOperations<String, String> valueOps;

        @InjectMocks
        private AuthService authService;

        private User testUser;

        @BeforeEach
        void setUp() {
                // Redis ValueOperations Mock 설정
                given(redis.opsForValue()).willReturn(valueOps);
                given(props.getAccessTtlSeconds()).willReturn(1800L);

                // 테스트 사용자 생성
                testUser = User.builder()
                                .id(1L)
                                .loginId("testUser")
                                .passwordHash("$2a$10$encodedPassword")
                                .role(Role.GUARDIAN)
                                .status(com.aicc.silverlink.domain.user.entity.UserStatus.ACTIVE)
                                .build();
        }

        @Test
        @DisplayName("로그인 성공 - 정상적인 사용자 인증 및 토큰 발급")
        void login_Success() {
                // given
                AuthDtos.LoginRequest request = new AuthDtos.LoginRequest("testUser", "password123!");
                SessionService.SessionIssue sessionIssued = new SessionService.SessionIssue("sid-123",
                                "refresh-token-abc");

                given(valueOps.get("loginfail: testUser")).willReturn(null);
                given(userRepository.findByLoginId("testUser")).willReturn(Optional.of(testUser));
                given(passwordEncoder.matches("password123!", testUser.getPasswordHash())).willReturn(true);
                given(sessionService.issueSession(testUser.getId(), testUser.getRole())).willReturn(sessionIssued);
                given(jwt.createAccessToken(testUser.getId(), testUser.getRole(), "sid-123", 1800L))
                                .willReturn("access-token-xyz");

                // when
                AuthService.AuthResult result = authService.login(request);

                // then
                assertThat(result).isNotNull();
                assertThat(result.accessToken()).isEqualTo("access-token-xyz");
                assertThat(result.refreshToken()).isEqualTo("refresh-token-abc");
                assertThat(result.sid()).isEqualTo("sid-123");
                assertThat(result.ttl()).isEqualTo(1800L);

                verify(redis).delete("loginfail: testUser");
                verify(sessionService).issueSession(testUser.getId(), testUser.getRole());
        }

        @Test
        @DisplayName("로그인 실패 - 사용자를 찾을 수 없음")
        void login_Fail_UserNotFound() {
                // given
                AuthDtos.LoginRequest request = new AuthDtos.LoginRequest("unknownUser", "password123!");

                given(valueOps.get("loginfail: unknownUser")).willReturn(null);
                given(userRepository.findByLoginId("unknownUser")).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> authService.login(request))
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessage("LOGIN_FAIL");
        }

        @Test
        @DisplayName("로그인 실패 - 비밀번호 불일치 (실패 카운트 증가)")
        void login_Fail_WrongPassword() {
                // given
                AuthDtos.LoginRequest request = new AuthDtos.LoginRequest("testUser", "wrongPassword");

                given(valueOps.get("loginfail: testUser")).willReturn(null);
                given(userRepository.findByLoginId("testUser")).willReturn(Optional.of(testUser));
                given(passwordEncoder.matches("wrongPassword", testUser.getPasswordHash())).willReturn(false);

                // when & then
                assertThatThrownBy(() -> authService.login(request))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("LOGIN_FAIL");

                verify(valueOps).increment("loginfail: testUser");
                verify(redis).expire("loginfail: testUser", 15, TimeUnit.MINUTES);
        }

        @Test
        @DisplayName("로그인 실패 - 브루트포스 방어 (10회 이상 실패)")
        void login_Fail_TooManyAttempts() {
                // given
                AuthDtos.LoginRequest request = new AuthDtos.LoginRequest("testUser", "password123!");

                given(valueOps.get("loginfail: testUser")).willReturn("10");

                // when & then
                assertThatThrownBy(() -> authService.login(request))
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessage("TOO_MANY_ATTEMPS");

                verify(userRepository, never()).findByLoginId(anyString());
        }

        @Test
        @DisplayName("로그인 실패 - 비활성 사용자")
        void login_Fail_InactiveUser() {
                // given
                AuthDtos.LoginRequest request = new AuthDtos.LoginRequest("testUser", "password123!");
                User inactiveUser = User.builder()
                                .id(1L)
                                .loginId("testUser")
                                .passwordHash("$2a$10$encodedPassword")
                                .role(Role.GUARDIAN)
                                .status(com.aicc.silverlink.domain.user.entity.UserStatus.LOCKED) // 비활성 상태
                                .build();

                given(valueOps.get("loginfail: testUser")).willReturn(null);
                given(userRepository.findByLoginId("testUser")).willReturn(Optional.of(inactiveUser));

                // when & then
                assertThatThrownBy(() -> authService.login(request))
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessage("USER_INACTIVE");
        }

        @Test
        @DisplayName("토큰 갱신 성공 - 새로운 액세스 토큰 및 리프레시 토큰 발급")
        void refresh_Success() {
                // given
                String sid = "sid-123";
                String oldRefreshToken = "old-refresh-token";
                String newRefreshToken = "new-refresh-token";

                Map<String, String> sessionMeta = Map.of(
                                "userId", "1",
                                "role", "GUARDIAN");

                given(sessionService.rotateRefresh(sid, oldRefreshToken)).willReturn(newRefreshToken);
                given(sessionService.getSessionMeta(sid)).willReturn(sessionMeta);
                given(jwt.createAccessToken(1L, Role.GUARDIAN, sid, 1800L))
                                .willReturn("new-access-token");

                // when
                AuthService.AuthResult result = authService.refresh(sid, oldRefreshToken);

                // then
                assertThat(result).isNotNull();
                assertThat(result.accessToken()).isEqualTo("new-access-token");
                assertThat(result.refreshToken()).isEqualTo(newRefreshToken);
                assertThat(result.sid()).isEqualTo(sid);
                assertThat(result.ttl()).isEqualTo(1800L);

                verify(sessionService).rotateRefresh(sid, oldRefreshToken);
                verify(sessionService).getSessionMeta(sid);
        }

        @Test
        @DisplayName("사용자 ID로 토큰 발급 성공")
        void issueForUser_Success() {
                // given
                Long userId = 1L;
                HttpServletRequest mockRequest = mock(HttpServletRequest.class);
                SessionService.SessionIssue sessionIssued = new SessionService.SessionIssue("sid-456",
                                "refresh-token-def");

                given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
                given(sessionService.issueSession(userId, testUser.getRole())).willReturn(sessionIssued);
                given(jwt.createAccessToken(userId, testUser.getRole(), "sid-456", 1800L))
                                .willReturn("access-token-for-user");

                // when
                AuthService.AuthResult result = authService.issueForUser(userId, mockRequest);

                // then
                assertThat(result).isNotNull();
                assertThat(result.accessToken()).isEqualTo("access-token-for-user");
                assertThat(result.refreshToken()).isEqualTo("refresh-token-def");
                assertThat(result.sid()).isEqualTo("sid-456");
                assertThat(result.ttl()).isEqualTo(1800L);

                verify(sessionService).issueSession(userId, testUser.getRole());
        }

        @Test
        @DisplayName("사용자 ID로 토큰 발급 실패 - 사용자 없음")
        void issueForUser_Fail_UserNotFound() {
                // given
                Long userId = 999L;
                HttpServletRequest mockRequest = mock(HttpServletRequest.class);

                given(userRepository.findById(userId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> authService.issueForUser(userId, mockRequest))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("USER_NOT_FOUND");
        }

        @Test
        @DisplayName("사용자 ID로 토큰 발급 실패 - 비활성 사용자")
        void issueForUser_Fail_InactiveUser() {
                // given
                Long userId = 1L;
                HttpServletRequest mockRequest = mock(HttpServletRequest.class);
                User inactiveUser = User.builder()
                                .id(1L)
                                .loginId("testUser")
                                .passwordHash("$2a$10$encodedPassword")
                                .role(Role.GUARDIAN)
                                .status(com.aicc.silverlink.domain.user.entity.UserStatus.LOCKED) // 비활성 상태
                                .build();

                given(userRepository.findById(userId)).willReturn(Optional.of(inactiveUser));

                // when & then
                assertThatThrownBy(() -> authService.issueForUser(userId, mockRequest))
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessage("USER_INACTIVE");
        }

        @Test
        @DisplayName("로그아웃 성공 - 세션 무효화")
        void logout_Success() {
                // given
                String sid = "sid-123";

                // when
                authService.logout(sid);

                // then
                verify(sessionService).invalidateBySid(sid);
        }
}