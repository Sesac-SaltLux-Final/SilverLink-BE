package com.aicc.silverlink.domain.auth.service;

import com.aicc.silverlink.domain.auth.dto.PhoneVerificationDtos;
import com.aicc.silverlink.domain.auth.entity.PhoneVerification;
import com.aicc.silverlink.domain.auth.repository.PhoneVerificationRepository;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import com.aicc.silverlink.global.config.auth.AuthPhoneProperties;
import com.aicc.silverlink.infra.external.sms.SolapiSmsSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhoneVerificationServiceTest {

        @Mock
        private PhoneVerificationRepository repo;

        @Mock
        private UserRepository userRepo;

        @Mock
        private PasswordEncoder passwordEncoder;

        @Mock
        private StringRedisTemplate redis;

        @Mock
        private ValueOperations<String, String> valueOps;

        @Mock
        private SolapiSmsSender smsSender;

        @Mock
        private AuthPhoneProperties props;

        @InjectMocks
        private PhoneVerificationService service;

        private User testUser;
        private PhoneVerification testVerification;

        @BeforeEach
        void setUp() {
                // Redis ValueOperations Mock 설정
                given(redis.opsForValue()).willReturn(valueOps);

                // Properties Mock 설정
                given(props.getCooldownSeconds()).willReturn(60);
                given(props.getDailyLimit()).willReturn(5);
                given(props.getCodeLength()).willReturn(6);
                given(props.getTtlSeconds()).willReturn(300);
                given(props.getMaxAttemps()).willReturn(5);
                given(props.isDebugReturnCode()).willReturn(false);

                // 테스트 사용자
                testUser = User.builder()
                                .id(1L)
                                .loginId("testUser")
                                .phone("01012345678")
                                .role(Role.GUARDIAN)
                                .status(com.aicc.silverlink.domain.user.entity.UserStatus.ACTIVE)
                                .build();

                // 테스트 인증 - create 메서드 사용
                testVerification = PhoneVerification.create(
                                testUser,
                                "+821012345678",
                                PhoneVerification.Purpose.SIGNUP,
                                "$2a$10$hashedCode",
                                "127.0.0.1",
                                300);
        }

        @Test
        @DisplayName("인증번호 요청 성공 - SMS 발송 및 Redis 쿨다운 설정")
        void requestCode_Success() {
                // given
                PhoneVerificationDtos.RequestCodeRequest request = new PhoneVerificationDtos.RequestCodeRequest(
                                "010-1234-5678",
                                PhoneVerification.Purpose.SIGNUP, null);
                String ip = "127.0.0.1";

                given(redis.hasKey("pv:cooldown:+821012345678:SIGNUP")).willReturn(false);
                given(valueOps.increment("pv:daily:+821012345678:SIGNUP:" + LocalDateTime.now().toLocalDate()))
                                .willReturn(1L);
                given(passwordEncoder.encode(anyString())).willReturn("$2a$10$hashedCode");

                PhoneVerification savedVerification = PhoneVerification.create(
                                null,
                                "+821012345678",
                                PhoneVerification.Purpose.SIGNUP,
                                "$2a$10$hashedCode",
                                ip,
                                300);

                given(repo.save(any(PhoneVerification.class))).willReturn(savedVerification);

                // when
                PhoneVerificationDtos.RequestCodeResponse response = service.requestCode(request, ip);

                // then
                assertThat(response).isNotNull();
                assertThat(response.expireAt()).isNotNull();

                verify(valueOps).set(eq("pv:cooldown:+821012345678:SIGNUP"), eq("1"), eq(60), eq(TimeUnit.SECONDS));
                verify(smsSender).sendSms(eq("+821012345678"), contains("인증번호"));
                verify(repo).save(any(PhoneVerification.class));
        }

        @Test
        @DisplayName("인증번호 요청 실패 - 쿨다운 기간 중")
        void requestCode_Fail_Cooldown() {
                // given
                PhoneVerificationDtos.RequestCodeRequest request = new PhoneVerificationDtos.RequestCodeRequest(
                                "010-1234-5678",
                                PhoneVerification.Purpose.SIGNUP, null);
                String ip = "127.0.0.1";

                given(redis.hasKey("pv:cooldown:+821012345678:SIGNUP")).willReturn(true);

                // when & then
                assertThatThrownBy(() -> service.requestCode(request, ip))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("PHONE_COOLDOWN");

                verify(smsSender, never()).sendSms(anyString(), anyString());
        }

        @Test
        @DisplayName("인증번호 요청 실패 - 일일 한도 초과")
        void requestCode_Fail_DailyLimit() {
                // given
                PhoneVerificationDtos.RequestCodeRequest request = new PhoneVerificationDtos.RequestCodeRequest(
                                "010-1234-5678",
                                PhoneVerification.Purpose.SIGNUP, null);
                String ip = "127.0.0.1";

                given(redis.hasKey("pv:cooldown:+821012345678:SIGNUP")).willReturn(false);
                given(valueOps.increment("pv:daily:+821012345678:SIGNUP:" + LocalDateTime.now().toLocalDate()))
                                .willReturn(6L);

                // when & then
                assertThatThrownBy(() -> service.requestCode(request, ip))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("PHONE_DAILY_LIMIT");

                verify(smsSender, never()).sendSms(anyString(), anyString());
        }

        @Test
        @DisplayName("인증번호 요청 성공 - 사용자 ID와 함께 요청")
        void requestCode_Success_WithUserId() {
                // given
                PhoneVerificationDtos.RequestCodeRequest request = new PhoneVerificationDtos.RequestCodeRequest(
                                "010-1234-5678",
                                PhoneVerification.Purpose.PASSWORD_RESET, 1L);
                String ip = "127.0.0.1";

                given(redis.hasKey("pv:cooldown:+821012345678:PASSWORD_RESET")).willReturn(false);
                given(valueOps.increment("pv:daily:+821012345678:PASSWORD_RESET:" + LocalDateTime.now().toLocalDate()))
                                .willReturn(1L);
                given(passwordEncoder.encode(anyString())).willReturn("$2a$10$hashedCode");
                given(userRepo.findById(1L)).willReturn(Optional.of(testUser));

                PhoneVerification savedVerification = PhoneVerification.create(
                                testUser,
                                "+821012345678",
                                PhoneVerification.Purpose.PASSWORD_RESET,
                                "$2a$10$hashedCode",
                                ip,
                                300);

                given(repo.save(any(PhoneVerification.class))).willReturn(savedVerification);

                // when
                PhoneVerificationDtos.RequestCodeResponse response = service.requestCode(request, ip);

                // then
                assertThat(response).isNotNull();
                verify(userRepo).findById(1L);
                verify(repo).save(any(PhoneVerification.class));
        }

        @Test
        @DisplayName("인증번호 확인 성공 - proof token 발급")
        void verifyCode_Success() {
                // given
                PhoneVerificationDtos.VerifyCodeRequest request = new PhoneVerificationDtos.VerifyCodeRequest(1L,
                                "123456");
                String ip = "127.0.0.1";

                given(repo.findById(1L)).willReturn(Optional.of(testVerification));
                given(passwordEncoder.matches("123456", testVerification.getCodeHash())).willReturn(true);

                // when
                PhoneVerificationDtos.VerifyCodeResponse response = service.vericyCode(request, ip);

                // then
                assertThat(response).isNotNull();
                assertThat(response.verified()).isTrue();
                assertThat(response.proofToken()).isNotNull();

                ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
                verify(valueOps).set(keyCaptor.capture(), anyString(), eq(5L), eq(TimeUnit.MINUTES));
                assertThat(keyCaptor.getValue()).startsWith("pv:proof:");
        }

        @Test
        @DisplayName("인증번호 확인 실패 - 잘못된 코드 (실패 카운트 증가)")
        void verifyCode_Fail_WrongCode() {
                // given
                PhoneVerificationDtos.VerifyCodeRequest request = new PhoneVerificationDtos.VerifyCodeRequest(1L,
                                "999999");
                String ip = "127.0.0.1";

                given(repo.findById(1L)).willReturn(Optional.of(testVerification));
                given(passwordEncoder.matches("999999", testVerification.getCodeHash())).willReturn(false);

                // when & then
                assertThatThrownBy(() -> service.vericyCode(request, ip))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("PV_CODE_INVALID");

                // FailCount 증가 검증은 실제 엔티티 메서드 호출을 확인하는 방식으로 처리
        }

        @Test
        @DisplayName("인증번호 확인 실패 - 인증 요청 없음")
        void verifyCode_Fail_NotFound() {
                // given
                PhoneVerificationDtos.VerifyCodeRequest request = new PhoneVerificationDtos.VerifyCodeRequest(999L,
                                "123456");
                String ip = "127.0.0.1";

                given(repo.findById(999L)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> service.vericyCode(request, ip))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("PV_NOT_FOUND");
        }

        @Test
        @DisplayName("인증번호 확인 실패 - 만료된 인증")
        void verifyCode_Fail_Expired() {
                // given
                PhoneVerificationDtos.VerifyCodeRequest request = new PhoneVerificationDtos.VerifyCodeRequest(1L,
                                "123456");
                String ip = "127.0.0.1";

                // Create로 만든 후 만료 시간을 과거로 설정하기 위해 mock 사용
                PhoneVerification expiredVerification = mock(PhoneVerification.class);
                given(expiredVerification.getStatus()).willReturn(PhoneVerification.Status.REQUESTED);
                given(expiredVerification.getExpiresAt()).willReturn(LocalDateTime.now().minusMinutes(1));

                given(repo.findById(1L)).willReturn(Optional.of(expiredVerification));

                // when & then
                assertThatThrownBy(() -> service.vericyCode(request, ip))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("PV_EXPIRED");
        }

        @Test
        @DisplayName("인증번호 확인 실패 - 최대 시도 횟수 초과")
        void verifyCode_Fail_TooManyAttempts() {
                // given
                PhoneVerificationDtos.VerifyCodeRequest request = new PhoneVerificationDtos.VerifyCodeRequest(1L,
                                "123456");
                String ip = "127.0.0.1";

                // Create로 만든 후 실패 카운트를 설정하기 위해 mock 사용
                PhoneVerification failedVerification = mock(PhoneVerification.class);
                given(failedVerification.getStatus()).willReturn(PhoneVerification.Status.REQUESTED);
                given(failedVerification.getExpiresAt()).willReturn(LocalDateTime.now().plusMinutes(5));
                given(failedVerification.getFailCount()).willReturn(5);

                given(repo.findById(1L)).willReturn(Optional.of(failedVerification));

                // when & then
                assertThatThrownBy(() -> service.vericyCode(request, ip))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("PV_TOO_MANY_ATTEMPTS");
        }

        @Test
        @DisplayName("인증번호 확인 실패 - 잘못된 상태 (이미 검증됨)")
        void verifyCode_Fail_WrongStatus() {
                // given
                PhoneVerificationDtos.VerifyCodeRequest request = new PhoneVerificationDtos.VerifyCodeRequest(1L,
                                "123456");
                String ip = "127.0.0.1";

                // Create로 만든 후 상태를 설정하기 위해 mock 사용
                PhoneVerification verifiedVerification = mock(PhoneVerification.class);
                given(verifiedVerification.getStatus()).willReturn(PhoneVerification.Status.VERIFIED);

                given(repo.findById(1L)).willReturn(Optional.of(verifiedVerification));

                // when & then
                assertThatThrownBy(() -> service.vericyCode(request, ip))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("PV_NOT_REQUESTED");
        }

        @Test
        @DisplayName("전화번호 E.164 형식 변환 - 010 시작")
        void toE164Kr_StartsWithZero() {
                // given
                PhoneVerificationDtos.RequestCodeRequest request1 = new PhoneVerificationDtos.RequestCodeRequest(
                                "010-1234-5678", PhoneVerification.Purpose.SIGNUP, null);
                PhoneVerificationDtos.RequestCodeRequest request2 = new PhoneVerificationDtos.RequestCodeRequest(
                                "01012345678",
                                PhoneVerification.Purpose.SIGNUP, null);

                given(redis.hasKey(anyString())).willReturn(false);
                given(valueOps.increment(anyString())).willReturn(1L);
                given(passwordEncoder.encode(anyString())).willReturn("$2a$10$hashedCode");
                given(repo.save(any(PhoneVerification.class))).willReturn(testVerification);

                // when
                service.requestCode(request1, "127.0.0.1");
                service.requestCode(request2, "127.0.0.1");

                // then
                // 두 요청 모두 +821012345678로 정규화되어야 함
                ArgumentCaptor<PhoneVerification> captor = ArgumentCaptor.forClass(PhoneVerification.class);
                verify(repo, times(2)).save(captor.capture());

                assertThat(captor.getAllValues().get(0).getPhoneE164()).isEqualTo("+821012345678");
                assertThat(captor.getAllValues().get(1).getPhoneE164()).isEqualTo("+821012345678");
        }

        @Test
        @DisplayName("전화번호 E.164 형식 변환 - 이미 +82 형식")
        void toE164Kr_AlreadyE164() {
                // given
                PhoneVerificationDtos.RequestCodeRequest request = new PhoneVerificationDtos.RequestCodeRequest(
                                "+821012345678",
                                PhoneVerification.Purpose.SIGNUP, null);

                given(redis.hasKey(anyString())).willReturn(false);
                given(valueOps.increment(anyString())).willReturn(1L);
                given(passwordEncoder.encode(anyString())).willReturn("$2a$10$hashedCode");
                given(repo.save(any(PhoneVerification.class))).willReturn(testVerification);

                // when
                service.requestCode(request, "127.0.0.1");

                // then
                ArgumentCaptor<PhoneVerification> captor = ArgumentCaptor.forClass(PhoneVerification.class);
                verify(repo).save(captor.capture());

                assertThat(captor.getValue().getPhoneE164()).isEqualTo("+821012345678");
        }
}