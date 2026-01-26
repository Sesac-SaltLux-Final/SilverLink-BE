package com.aicc.silverlink.domain.auth.service;

import com.aicc.silverlink.domain.auth.dto.AuthDtos;
import com.aicc.silverlink.domain.session.service.SessionService;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import com.aicc.silverlink.global.config.auth.AuthPolicyProperties;
import com.aicc.silverlink.global.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwt;
    private final SessionService sessionService;
    private final AuthPolicyProperties props;
    private final StringRedisTemplate redis;

    private String failKey(String loginId) {
        return "loginfail: " + loginId;
    }

    // 서비스 내부용 DTO ( Access 토큰 + Refresh 토큰 반환용)
    public record AuthResult(String accessToken, String refreshToken, String sid, long ttl, Role role) {
    }

    @Transactional
    public AuthResult login(AuthDtos.LoginRequest req) {
        // Brute-force 방어
        String fk = failKey(req.loginId());
        String failCntStr = redis.opsForValue().get(fk);
        int failCnt = failCntStr == null ? 0 : Integer.parseInt(failCntStr);
        if (failCnt >= 10)
            throw new IllegalStateException("TOO_MANY_ATTEMPS");

        User user = userRepository.findByLoginId(req.loginId())
                .orElseThrow(() -> new IllegalStateException("LOGIN_FAIL"));

        if (!user.isActive())
            throw new IllegalStateException("USER_INACTIVE");

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            redis.opsForValue().increment(fk);
            redis.expire(fk, 15, TimeUnit.MINUTES);
            throw new IllegalArgumentException("LOGIN_FAIL");
        }

        redis.delete(fk);

        // 세션 발급 ( Redis에 저장 )
        var issued = sessionService.issueSession(user.getId(), user.getRole());

        String access = jwt.createAccessToken(user.getId(), user.getRole(), issued.sid(), props.getAccessTtlSeconds());

        user.updateLastLogin();

        return new AuthResult(access, issued.refreshToken(), issued.sid(), props.getAccessTtlSeconds(), user.getRole());
    }

    public AuthResult refresh(String sid, String refreshToken) {
        String newRefreshToken = sessionService.rotateRefresh(sid, refreshToken);

        Map<String, String> meta = sessionService.getSessionMeta(sid);
        Long userId = Long.valueOf(meta.get("userId"));
        Role role = Role.valueOf(meta.get("role"));

        long ttl = props.getAccessTtlSeconds();
        String newAccessToken = jwt.createAccessToken(userId, role, sid, ttl);

        return new AuthResult(newAccessToken, newRefreshToken, sid, ttl, role);
    }

    @Transactional
    public AuthResult issueForUser(Long userId, HttpServletRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        if (!user.isActive()) {
            throw new IllegalStateException("USER_INACTIVE");
        }

        var issued = sessionService.issueSession(user.getId(), user.getRole());

        String accessToken = jwt.createAccessToken(
                user.getId(),
                user.getRole(),
                issued.sid(),
                props.getAccessTtlSeconds());

        user.updateLastLogin();

        return new AuthResult(
                accessToken,
                issued.refreshToken(),
                issued.sid(),
                props.getAccessTtlSeconds(),
                user.getRole());
    }

    public void logout(String sid) {
        sessionService.invalidateBySid(sid);
    }

    /**
     * 휴대폰 인증 후 로그인
     * proofToken을 검증하고 해당 휴대폰 번호의 사용자로 세션 발급
     */
    @Transactional
    public AuthResult loginWithPhone(String phone, String proofToken) {
        // proofToken 검증 (Redis에서 확인)
        String proofKey = "pv:proof:" + proofToken;
        String storedPhone = redis.opsForValue().get(proofKey);

        if (storedPhone == null) {
            throw new IllegalArgumentException("INVALID_PROOF_TOKEN");
        }

        // 전화번호 형식 정규화 (+82 또는 0으로 시작하는 형태 모두 처리)
        String normalizedPhone = normalizePhone(phone);
        String normalizedStoredPhone = normalizePhone(storedPhone);

        if (!normalizedPhone.equals(normalizedStoredPhone)) {
            throw new IllegalArgumentException("PHONE_MISMATCH");
        }

        // proofToken 사용 후 삭제
        redis.delete(proofKey);

        // 해당 휴대폰 번호의 사용자 조회
        User user = userRepository.findByPhone(normalizedPhone)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        if (!user.isActive()) {
            throw new IllegalStateException("USER_INACTIVE");
        }

        // 세션 발급
        var issued = sessionService.issueSession(user.getId(), user.getRole());

        String accessToken = jwt.createAccessToken(
                user.getId(),
                user.getRole(),
                issued.sid(),
                props.getAccessTtlSeconds());

        user.updateLastLogin();

        return new AuthResult(
                accessToken,
                issued.refreshToken(),
                issued.sid(),
                props.getAccessTtlSeconds(),
                user.getRole());
    }

    private String normalizePhone(String phone) {
        if (phone == null)
            return "";
        // 숫자만 추출
        String digits = phone.replaceAll("[^0-9]", "");
        // +82로 시작하면 0으로 변환
        if (digits.startsWith("82")) {
            digits = "0" + digits.substring(2);
        }
        return digits;
    }

    /**
     * 비밀번호 재설정
     * proofToken으로 휴대폰 인증된 사용자의 비밀번호를 변경
     */
    @Transactional
    public void resetPassword(String loginId, String proofToken, String newPassword) {
        // proofToken 검증
        String proofKey = "pv:proof:" + proofToken;
        String storedPhone = redis.opsForValue().get(proofKey);

        if (storedPhone == null) {
            throw new IllegalArgumentException("INVALID_PROOF_TOKEN");
        }

        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        // 해당 사용자의 휴대폰 번호와 proofToken의 번호가 일치하는지 확인
        String normalizedStoredPhone = normalizePhone(storedPhone);
        String normalizedUserPhone = normalizePhone(user.getPhone());

        if (!normalizedStoredPhone.equals(normalizedUserPhone)) {
            throw new IllegalArgumentException("PHONE_MISMATCH");
        }

        // proofToken 삭제
        redis.delete(proofKey);

        // 비밀번호 변경 (User 엔티티의 기존 changePassword 메서드 사용)
        user.changePassword(passwordEncoder.encode(newPassword));

        // 현재 사용자의 기존 세션 무효화 (user:userId:sid 키 기반)
        String userSidKey = "user:" + user.getId() + ":sid";
        String existingSid = redis.opsForValue().get(userSidKey);
        if (existingSid != null) {
            sessionService.invalidateBySid(existingSid);
        }
    }

    /**
     * 아이디 찾기
     * 이름과 proofToken으로 마스킹된 로그인 ID 반환
     */
    public String findMaskedLoginId(String name, String proofToken) {
        // proofToken 검증
        String proofKey = "pv:proof:" + proofToken;
        String storedPhone = redis.opsForValue().get(proofKey);

        if (storedPhone == null) {
            throw new IllegalArgumentException("INVALID_PROOF_TOKEN");
        }

        String normalizedPhone = normalizePhone(storedPhone);

        // proofToken 삭제
        redis.delete(proofKey);

        // 이름과 휴대폰 번호로 사용자 찾기
        User user = userRepository.findByNameAndPhone(name, normalizedPhone)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        // 로그인 ID 마스킹 (앞 4글자만 표시)
        String loginId = user.getLoginId();
        if (loginId.length() <= 4) {
            return loginId.substring(0, 1) + "***";
        }
        return loginId.substring(0, loginId.length() - 3) + "***";
    }

    /**
     * 로그인 확인 (기존 세션 체크)
     * 기존 세션이 있으면 임시 토큰 반환, 없으면 바로 로그인
     */
    @Transactional
    public LoginCheckResult checkLogin(AuthDtos.LoginRequest req) {
        // Brute-force 방어
        String fk = failKey(req.loginId());
        String failCntStr = redis.opsForValue().get(fk);
        int failCnt = failCntStr == null ? 0 : Integer.parseInt(failCntStr);
        if (failCnt >= 10)
            throw new IllegalStateException("TOO_MANY_ATTEMPS");

        User user = userRepository.findByLoginId(req.loginId())
                .orElseThrow(() -> new IllegalStateException("LOGIN_FAIL"));

        if (!user.isActive())
            throw new IllegalStateException("USER_INACTIVE");

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            redis.opsForValue().increment(fk);
            redis.expire(fk, 15, TimeUnit.MINUTES);
            throw new IllegalArgumentException("LOGIN_FAIL");
        }

        redis.delete(fk);

        // 기존 세션 확인
        String existingSid = sessionService.hasExistingSession(user.getId());
        
        if (existingSid != null) {
            // 기존 세션 있음 - 임시 토큰 발급
            String loginToken = sessionService.createLoginToken(user.getId());
            return new LoginCheckResult(true, loginToken, null);
        }

        // 기존 세션 없음 - 바로 로그인
        var issued = sessionService.issueSession(user.getId(), user.getRole());
        String access = jwt.createAccessToken(user.getId(), user.getRole(), issued.sid(), props.getAccessTtlSeconds());
        user.updateLastLogin();

        AuthResult authResult = new AuthResult(access, issued.refreshToken(), issued.sid(), props.getAccessTtlSeconds(), user.getRole());
        return new LoginCheckResult(false, null, authResult);
    }

    /**
     * 강제 로그인 (기존 세션 종료 후 로그인)
     */
    @Transactional
    public AuthResult forceLogin(String loginToken) {
        Long userId = sessionService.validateLoginToken(loginToken);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        if (!user.isActive())
            throw new IllegalStateException("USER_INACTIVE");

        // 기존 세션 강제 종료
        sessionService.forceKickExistingSession(userId);

        // 새 세션 발급
        var issued = sessionService.issueSession(user.getId(), user.getRole());
        String access = jwt.createAccessToken(user.getId(), user.getRole(), issued.sid(), props.getAccessTtlSeconds());
        user.updateLastLogin();

        return new AuthResult(access, issued.refreshToken(), issued.sid(), props.getAccessTtlSeconds(), user.getRole());
    }

    /**
     * 로그인 확인 결과
     */
    public record LoginCheckResult(
        boolean needsConfirmation,
        String loginToken,
        AuthResult authResult
    ) {}

}
