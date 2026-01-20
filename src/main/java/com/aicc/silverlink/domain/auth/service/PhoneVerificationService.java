package com.aicc.silverlink.domain.auth.service;

import com.aicc.silverlink.domain.auth.dto.PhoneVerificationDtos;
import com.aicc.silverlink.domain.auth.entity.PhoneVerification;
import com.aicc.silverlink.domain.auth.repository.PhoneVerificationRepository;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import com.aicc.silverlink.global.config.auth.AuthPhoneProperties;
import com.aicc.silverlink.infra.external.sms.SolapiSmsSender;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PhoneVerificationService {

    private final PhoneVerificationRepository repo;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redis;
    private final SolapiSmsSender smsSender;
    private final AuthPhoneProperties props;

    private static final SecureRandom RND = new SecureRandom();
    private final UserDetailsManager userDetailsManager;

    @Transactional
    public PhoneVerificationDtos.RequestCodeResponse requestCode(PhoneVerificationDtos.RequestCodeRequest req, String ip) {
        String phoneE164 = toE164Kr(req.phone());

        String cooldownKey = "pv:cooldown:" + phoneE164 + ":" + req.purpose();
        if(Boolean.TRUE.equals(redis.hasKey(cooldownKey))){
            throw new IllegalArgumentException("PHONE_COOLDOWN");
        }

        String dailyKey = "pv:daily:" + phoneE164 + ":" + req.purpose() + ":" + LocalDateTime.now().toLocalDate();
        Long dailyCount = redis.opsForValue().increment(dailyKey);
        if(dailyCount != null && dailyCount == 1){
            redis.expire(dailyKey,2, TimeUnit.DAYS);
        }
        if(dailyCount != null && dailyCount > props.getDailyLimit()) {
            throw new IllegalArgumentException("PHONE_DAILY_LIMIT");
        }

        redis.opsForValue().set(cooldownKey, "1" , props.getCooldownSeconds(), TimeUnit.SECONDS);

        String code = genNumeric(props.getCodeLength());
        String codeHash = passwordEncoder.encode(code);

        User user = null;

        if(req.userId() != null) {
            user = userRepo.findById(req.userId())
                    .orElseThrow(()-> new IllegalArgumentException("USER_NOT_FOUND"));
        }

        PhoneVerification pv = PhoneVerification.create(
                user,
                phoneE164,
                req.purpose(),
                codeHash,
                ip,
                props.getTtlSeconds()
        );

        repo.save(pv);

        smsSender.sendSms(phoneE164,"[SilverLink] 인증번호는 " + code + "입니다. (" + (props.getTtlSeconds() / 60) + "분 이내");

        return new PhoneVerificationDtos.RequestCodeResponse(
                pv.getId(),
                pv.getExpiresAt(),
                props.isDebugReturnCode() ? code : null
        );

    }

    @Transactional
    public PhoneVerificationDtos.VerifyCodeResponse verifyCode(PhoneVerificationDtos.VerifyCodeRequest req, String ip) {
        PhoneVerification pv = repo.findById(req.verificationId())
                .orElseThrow(()-> new IllegalArgumentException("PV_NOT_FOUND"));

        if (pv.getStatus() != PhoneVerification.Status.REQUESTED){
            throw new IllegalArgumentException("PV_NOT_REQUESTED");
        }

        if (LocalDateTime.now().isAfter(pv.getExpiresAt())){
            pv.expire();
            throw new IllegalArgumentException("PV_EXPIRED");
        }


        if (pv.getFailCount() >= props.getMaxAttemps()) {
            pv.fail();
            throw new IllegalArgumentException("PV_TOO_MANY_ATTEMPTS");
        }

        boolean ok = passwordEncoder.matches(req.code(), pv.getCodeHash());

        if (!ok) {
            pv.increaseFailCount();

            if(pv.getFailCount() >= props.getMaxAttemps()) {
                pv.fail();
            }
            throw new IllegalArgumentException("PV_CODE_INVALID");
        }

        pv.verify();

        if (pv.getUser() != null) {
            User user = pv.getUser();
            user.markPhoneVerified();
        }

        String proofToken = UUID.randomUUID().toString();
        String proofKey = "pv:proof:" + proofToken;

        redis.opsForValue().set(
                proofKey,
                pv.getPhoneE164(),
                5,
                TimeUnit.MINUTES
        );

        return new PhoneVerificationDtos.VerifyCodeResponse(true,proofToken);



    }


    private String toE164Kr(String raw){
        String digits = raw.replaceAll("[^0-9+]","");
        if(digits.startsWith("+")) return digits;
        if(digits.startsWith("0")) {
            String no0 = digits.substring(1);
            return "+82" + no0;
        }
        return "+82" + digits;
    }

    private String genNumeric(int len){
        int bound = (int) Math.pow(10, len);
        int n = RND.nextInt(bound);
        return String.format("%0" + len + "d", n);
    }
}
