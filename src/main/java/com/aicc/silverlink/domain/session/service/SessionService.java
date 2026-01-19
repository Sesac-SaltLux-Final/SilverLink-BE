package com.aicc.silverlink.domain.session.service;

import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.global.config.auth.AuthPolicyProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 로깅 추가 권장
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final StringRedisTemplate redis;
    private final AuthPolicyProperties props;

    private static String userSidKey(Long userId) { return "user:" + userId + ":sid"; }
    private static String sessKey(String sid) { return "sess:" + sid; }

    public record SessionIssue(String sid, String refreshToken) {}


    public SessionIssue issueSession(Long userId, Role role) {

        long idleSeconds = props.getIdleTtlSeconds();
        String policy = props.getConcurrentPolicy();

        String userKey = userSidKey(userId);
        String existingSid = redis.opsForValue().get(userKey);

        if (existingSid != null && Boolean.TRUE.equals(redis.hasKey(sessKey(existingSid)))) {
            if ("BLOCK_NEW".equalsIgnoreCase(policy)) {
                throw new IllegalStateException("ALREADY_LOGGED_IN");
            }
            invalidateBySid(existingSid);
        }

        String sid = UUID.randomUUID().toString();
        String refresh = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        long now = Instant.now().getEpochSecond();

        String sessionKey = sessKey(sid);

        Map<String, String> sessionData = Map.of(
                "userId", String.valueOf(userId),
                "role", role.name(),
                "refreshHash", sha256(refresh),
                "lastSeen", String.valueOf(now)
        );
        redis.opsForHash().putAll(sessionKey, sessionData);  // putAll을 사용하여 네트워크 왕복 횟수 줄이기

        //  TTL 설정 (두 키 모두 설정해야 함)
        redis.expire(sessionKey, idleSeconds, TimeUnit.SECONDS);
        redis.opsForValue().set(userKey, sid, idleSeconds, TimeUnit.SECONDS);

        return new SessionIssue(sid, refresh);
    }


    public Map<String, String> getSessionMeta(String sid){
        if (Boolean.FALSE.equals(redis.hasKey(sessKey(sid)))){
            throw new IllegalStateException("SESSION_EXPIRED");
        }

        return redis.<String, String>opsForHash().entries(sessKey(sid));

    }


    public void touch(String sid) {
        if (sid == null) return;
        String sessionKey = sessKey(sid);

        if (Boolean.FALSE.equals(redis.hasKey(sessionKey))) return;

        long idleSeconds = props.getIdleTtlSeconds();

        redis.opsForHash().put(sessionKey, "lastSeen", String.valueOf(Instant.now().getEpochSecond()));
        redis.expire(sessionKey, idleSeconds, TimeUnit.SECONDS);

        Object userIdObj = redis.opsForHash().get(sessionKey, "userId");
        if (userIdObj != null) {
            String userId = (String) userIdObj;
            redis.expire(userSidKey(Long.valueOf(userId)), idleSeconds, TimeUnit.SECONDS);
        }
    }


    public boolean isActive(String sid, Long userId) {
        if (sid == null) return false;

        if (Boolean.FALSE.equals(redis.hasKey(sessKey(sid)))) return false;

        String mappedSid = redis.opsForValue().get(userSidKey(userId));
        return sid.equals(mappedSid);
    }


    public String rotateRefresh(String sid, String presentedRefresh) {
        String sessionKey = sessKey(sid);

        if (Boolean.FALSE.equals(redis.hasKey(sessionKey))) {
            throw new IllegalStateException("SESSION_EXPIRED");
        }

        String savedHash = (String) redis.opsForHash().get(sessionKey, "refreshHash");
        if (savedHash == null || !savedHash.equals(sha256(presentedRefresh))) {

            invalidateBySid(sid);
            throw new IllegalStateException("REFRESH_REUSED"); // GlobalExceptionHandler에서 401/403 처리
        }

        String newRefresh = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        redis.opsForHash().put(sessionKey, "refreshHash", sha256(newRefresh));

        touch(sid);

        return newRefresh;
    }

    public void invalidateBySid(String sid) {
        if (sid == null) return;
        String sessionKey = sessKey(sid);

        String userId = (String) redis.opsForHash().get(sessionKey, "userId");

        redis.delete(sessionKey);

        if (userId != null) {
            redis.delete(userSidKey(Long.valueOf(userId)));
        }
    }


    private static String sha256(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }
}