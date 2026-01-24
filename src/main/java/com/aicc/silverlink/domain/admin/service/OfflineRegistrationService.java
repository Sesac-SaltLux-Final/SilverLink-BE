package com.aicc.silverlink.domain.admin.service;

import com.aicc.silverlink.domain.admin.dto.AdminMemberDtos;
import com.aicc.silverlink.domain.admin.entity.OfflineRegistrationLog;
import com.aicc.silverlink.domain.admin.repository.OfflineRegistrationLogRepository;
import com.aicc.silverlink.domain.auth.entity.PhoneVerification;
import com.aicc.silverlink.domain.auth.repository.PhoneVerificationRepository;
import com.aicc.silverlink.domain.consent.entity.ConsentHistory;
import com.aicc.silverlink.domain.consent.repository.ConsentHistoryRepository;
import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.domain.elderly.repository.ElderlyRepository;
import com.aicc.silverlink.domain.guardian.entity.Guardian;
import com.aicc.silverlink.domain.guardian.entity.GuardianElderly;
import com.aicc.silverlink.domain.guardian.entity.RelationType;
import com.aicc.silverlink.domain.guardian.repository.GuardianElderlyRepository;
import com.aicc.silverlink.domain.guardian.repository.GuardianRepository;
import com.aicc.silverlink.domain.system.entity.AdministrativeDivision;
import com.aicc.silverlink.domain.system.repository.AdministrativeDivisionRepository;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import com.aicc.silverlink.global.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OfflineRegistrationService {

    private final UserRepository userRepository;
    private final ElderlyRepository elderlyRepository;
    private final GuardianRepository guardianRepository;
    private final GuardianElderlyRepository guardianElderlyRepository;
    private final AdministrativeDivisionRepository divRepository;
    private final OfflineRegistrationLogRepository logRepository;
    private final ConsentHistoryRepository consentRepository;
    private final PhoneVerificationRepository phoneVerificationRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long registerElderly(AdminMemberDtos.RegisterElderlyRequest req) {
        Long adminId = SecurityUtils.currentUserId();

        // 1. User 생성
        validateDuplicate(req.loginId(), req.phone());
        User user = User.createLocal(
                req.loginId(),
                passwordEncoder.encode(req.password()),
                req.name(),
                req.phone(),
                req.email(),
                Role.ELDERLY);
        user.markPhoneVerified(); // 대면 인증 처리
        userRepository.save(user);

        // 2. Elderly 프로필 생성
        AdministrativeDivision div = divRepository.findById(req.admCode())
                .orElseThrow(() -> new IllegalArgumentException("INVALID_ADM_CODE"));

        Elderly elderly = Elderly.create(user, div, req.birthDate(), req.gender());
        elderly.updateAddress(req.addressLine1(), req.addressLine2(), req.zipcode());
        elderlyRepository.save(elderly);

        // 3. 로그 기록
        saveLog(adminId, user.getId(), req.memo());
        saveVerification(user);
        saveConsent(user, "PRIVACY", "AGREE");
        saveConsent(user, "SENSITIVE_INFO", "AGREE");
        saveConsent(user, "MEDICATION", "AGREE"); // 기본 동의 간주

        return user.getId();
    }

    @Transactional
    public Long registerGuardian(AdminMemberDtos.RegisterGuardianRequest req) {
        Long adminId = SecurityUtils.currentUserId();

        // 1. User 생성
        validateDuplicate(req.loginId(), req.phone());
        User user = User.createLocal(
                req.loginId(),
                passwordEncoder.encode(req.password()),
                req.name(),
                req.phone(),
                req.email(),
                Role.GUARDIAN);
        user.markPhoneVerified();
        userRepository.save(user);

        // 2. Guardian 프로필
        Guardian guardian = Guardian.create(
                user,
                req.addressLine1(),
                req.addressLine2(),
                req.zipcode(),
                LocalDateTime.now());
        guardianRepository.save(guardian);

        // 3. 연결
        Elderly elderly = elderlyRepository.findById(req.elderlyUserId())
                .orElseThrow(() -> new IllegalArgumentException("ELDERLY_NOT_FOUND"));

        // Enum 변환
        RelationType relationType;
        try {
            relationType = RelationType.valueOf(req.relationType());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("INVALID_RELATION_TYPE");
        }

        GuardianElderly relation = GuardianElderly.create(
                guardian,
                elderly,
                relationType,
                LocalDateTime.now());
        guardianElderlyRepository.save(relation);

        // 4. 로그
        saveLog(adminId, user.getId(), req.memo());
        saveVerification(user);
        saveConsent(user, "PRIVACY", "AGREE");
        saveConsent(user, "THIRD_PARTY", "AGREE");

        return user.getId();
    }

    private void validateDuplicate(String loginId, String phone) {
        if (userRepository.findByLoginId(loginId).isPresent()) {
            throw new IllegalArgumentException("LOGIN_ID_DUPLICATE");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new IllegalArgumentException("PHONE_DUPLICATE");
        }
    }

    private void saveLog(Long adminId, Long targetId, String memo) {
        logRepository.save(OfflineRegistrationLog.builder()
                .registrarUserId(adminId)
                .targetUserId(targetId)
                .channel("CENTER_VISIT")
                .memo(memo)
                .build());
    }

    private void saveVerification(User user) {
        phoneVerificationRepository.save(PhoneVerification.builder()
                .user(user)
                .phoneE164(user.getPhone())
                .codeHash("OFFLINE")
                .status(PhoneVerification.Status.VERIFIED)
                .purpose(PhoneVerification.Purpose.SIGNUP)
                .failCount(0)
                .requestIp("OFFLINE")
                .build());
    }

    private void saveConsent(User user, String type, String action) {
        consentRepository.save(ConsentHistory.builder()
                .user(user)
                .consentType(type)
                .actionType(action)
                .consentDate(LocalDateTime.now())
                .ipAddress("OFFLINE")
                .userAgent("ADMIN_CONSOLE")
                .build());
    }
}
