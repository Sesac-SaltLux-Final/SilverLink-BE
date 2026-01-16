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

    // Redis Key 패턴 정의
    private static String userSidKey(Long userId) { return "user:" + userId + ":sid"; }
    private static String sessKey(String sid) { return "sess:" + sid; }

    public record SessionIssue(String sid, String refreshToken) {}

    /** * 로그인 성공 시 호출: 동시로그인 정책 처리 + 새 세션 발급
     */
    public SessionIssue issueSession(Long userId, Role role) {
        // 1. 설정값 가져오기
        long idleSeconds = props.getIdleTtlSeconds();
        String policy = props.getConcurrentPolicy();

        // 2. 기존 세션 확인 및 정책 적용
        String userKey = userSidKey(userId);
        String existingSid = redis.opsForValue().get(userKey);

        if (existingSid != null && Boolean.TRUE.equals(redis.hasKey(sessKey(existingSid)))) {
            if ("BLOCK_NEW".equalsIgnoreCase(policy)) {
                throw new IllegalStateException("ALREADY_LOGGED_IN");
            }
            // KICK_OLD: 기존 세션 삭제
            invalidateBySid(existingSid);
        }

        // 3. 새 세션 생성
        String sid = UUID.randomUUID().toString();
        String refresh = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        long now = Instant.now().getEpochSecond();

        String sessionKey = sessKey(sid);

        // 4. Redis 저장 (Hash)
        // putAll을 사용하여 네트워크 왕복 횟수 줄이기
        Map<String, String> sessionData = Map.of(
                "userId", String.valueOf(userId),
                "role", role.name(),
                "refreshHash", sha256(refresh),
                "lastSeen", String.valueOf(now)
        );
        redis.opsForHash().putAll(sessionKey, sessionData);

        // 5. TTL 설정 (가장 중요: 두 키 모두 설정해야 함)
        redis.expire(sessionKey, idleSeconds, TimeUnit.SECONDS);
        redis.opsForValue().set(userKey, sid, idleSeconds, TimeUnit.SECONDS);

        return new SessionIssue(sid, refresh);
    }

    /** * 세션에 저장된 유저 정보 조회 (토큰 재발급 용)
     */
    public Map<String, String> getSessionMeta(String sid){
        if (Boolean.FALSE.equals(redis.hasKey(sessKey(sid)))){
            throw new IllegalStateException("SESSION_EXPIRED");
        }

        return redis.<String, String>opsForHash().entries(sessKey(sid));

    }

    /** * 매 요청마다 호출: idle 연장 (수정 핵심: 매핑 키 TTL도 같이 연장해야 함)
     */
    public void touch(String sid) {
        if (sid == null) return;
        String sessionKey = sessKey(sid);

        // 세션이 존재하는지 확인
        if (Boolean.FALSE.equals(redis.hasKey(sessionKey))) return;

        long idleSeconds = props.getIdleTtlSeconds();

        // 1. 세션 정보 업데이트 (lastSeen)
        redis.opsForHash().put(sessionKey, "lastSeen", String.valueOf(Instant.now().getEpochSecond()));
        redis.expire(sessionKey, idleSeconds, TimeUnit.SECONDS);

        // 2.유저 매핑 키(user:{id}:sid)의 수명도 같이 연장
        // 이걸 안 하면 세션 데이터는 있는데, 유저가 누구인지 찾는 키가 만료되어 로그아웃됨.
        Object userIdObj = redis.opsForHash().get(sessionKey, "userId");
        if (userIdObj != null) {
            String userId = (String) userIdObj;
            redis.expire(userSidKey(Long.valueOf(userId)), idleSeconds, TimeUnit.SECONDS);
        }
    }

    /** * Access JWT 검증 후: 서버 세션 살아있는지 체크
     */
    public boolean isActive(String sid, Long userId) {
        if (sid == null) return false;

        // 1. 세션 데이터 자체가 살아있는지
        if (Boolean.FALSE.equals(redis.hasKey(sessKey(sid)))) return false;

        // 2. 해당 유저의 최신 SID가 지금 들어온 SID와 일치하는지 (중복 로그인 등으로 밀려나지 않았는지)
        String mappedSid = redis.opsForValue().get(userSidKey(userId));
        return sid.equals(mappedSid);
    }

    /** * Refresh 요청: sid + refreshToken 검증 후 refresh rotation
     */
    public String rotateRefresh(String sid, String presentedRefresh) {
        String sessionKey = sessKey(sid);

        if (Boolean.FALSE.equals(redis.hasKey(sessionKey))) {
            throw new IllegalStateException("SESSION_EXPIRED");
        }

        String savedHash = (String) redis.opsForHash().get(sessionKey, "refreshHash");
        if (savedHash == null || !savedHash.equals(sha256(presentedRefresh))) {
            // 토큰 탈취 감지: 즉시 해당 세션 무효화
            invalidateBySid(sid);
            throw new IllegalStateException("REFRESH_REUSED"); // GlobalExceptionHandler에서 401/403 처리
        }

        // RTR(Refresh Token Rotation): 새 리프레시 토큰 발급
        String newRefresh = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        redis.opsForHash().put(sessionKey, "refreshHash", sha256(newRefresh));

        // 세션 연장 처리
        touch(sid);

        return newRefresh;
    }

    /** * 로그아웃 / 강제 종료
     */
    public void invalidateBySid(String sid) {
        if (sid == null) return;
        String sessionKey = sessKey(sid);

        // userId를 먼저 가져와서 매핑 키도 지워야 함
        String userId = (String) redis.opsForHash().get(sessionKey, "userId");

        redis.delete(sessionKey); // 세션 데이터 삭제

        if (userId != null) {
            redis.delete(userSidKey(Long.valueOf(userId))); // 유저 매핑 정보 삭제
        }
    }



    // 간단한 해시 유틸
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