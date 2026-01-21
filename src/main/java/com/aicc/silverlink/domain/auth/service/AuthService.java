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

    private String failKey(String loginId) {return "loginfail: " + loginId; }

    // 서비스 내부용 DTO ( Access 토큰 + Refresh 토큰 반환용)
    public record AuthResult(String accessToken, String refreshToken,String sid, long ttl){}

    @Transactional
    public AuthResult login(AuthDtos.LoginRequest req) {
        // Brute-force 방어
        String fk = failKey(req.loginId());
        String failCntStr = redis.opsForValue().get(fk);
        int failCnt = failCntStr == null ? 0 : Integer.parseInt(failCntStr);
        if (failCnt >= 10) throw new IllegalStateException("TOO_MANY_ATTEMPS");

        User user = userRepository.findByLoginId(req.loginId())
                .orElseThrow(() -> new IllegalStateException("LOGIN_FAIL"));

        if(!user.isActive()) throw new IllegalStateException("USER_INACTIVE");

        if(!passwordEncoder.matches(req.password(), user.getPasswordHash())){
            redis.opsForValue().increment(fk);
            redis.expire(fk, 15, TimeUnit.MINUTES);
            throw new IllegalArgumentException("LOGIN_FAIL");
        }

        redis.delete(fk);

        // 세션 발급 ( Redis에 저장 )
        var issued = sessionService.issueSession(user.getId(), user.getRole());

        String access = jwt.createAccessToken(user.getId(),user.getRole(), issued.sid(), props.getAccessTtlSeconds());

        user.updateLastLogin();

        return new AuthResult(access, issued.refreshToken(),issued.sid(),props.getAccessTtlSeconds());
    }

    public AuthResult refresh(String sid, String refreshToken){
        String newRefreshToken = sessionService.rotateRefresh(sid, refreshToken);

        Map<String, String> meta = sessionService.getSessionMeta(sid);
        Long userId = Long.valueOf(meta.get("userId"));
        Role role = Role.valueOf(meta.get("role"));

        long ttl = props.getAccessTtlSeconds();
        String newAccessToken = jwt.createAccessToken(userId,role,sid,ttl);

        return new AuthResult(newAccessToken, newRefreshToken ,sid ,ttl);
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
                props.getAccessTtlSeconds()
        );

        user.updateLastLogin();

        return new AuthResult(
                accessToken,
                issued.refreshToken(),
                issued.sid(),
                props.getAccessTtlSeconds()
        );
    }



    public void logout(String sid){
        sessionService.invalidateBySid(sid);
    }


}
