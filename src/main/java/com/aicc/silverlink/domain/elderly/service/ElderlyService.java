package com.aicc.silverlink.domain.elderly.service;

import com.aicc.silverlink.domain.assignment.repository.AssignmentRepository;
import com.aicc.silverlink.domain.consent.entity.AccessRequest.AccessScope;
import com.aicc.silverlink.domain.consent.repository.AccessRequestRepository;
import com.aicc.silverlink.domain.elderly.dto.request.ElderlyCreateRequest;
import com.aicc.silverlink.domain.elderly.dto.request.HealthInfoUpdateRequest;
import com.aicc.silverlink.domain.elderly.dto.response.ElderlySummaryResponse;
import com.aicc.silverlink.domain.elderly.dto.response.HealthInfoResponse;
import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.domain.elderly.entity.ElderlyHealthInfo;
import com.aicc.silverlink.domain.elderly.repository.ElderlyRepository;
import com.aicc.silverlink.domain.elderly.repository.HealthInfoRepository;
import com.aicc.silverlink.domain.guardian.repository.GuardianElderlyRepository;
import com.aicc.silverlink.domain.system.entity.AdministrativeDivision;
import com.aicc.silverlink.domain.system.repository.AdministrativeDivisionRepository;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.entity.UserStatus;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 어르신 서비스
 *
 * 민감정보(건강정보) 접근 권한 체크 로직이 포함되어 있습니다.
 *
 * 접근 권한 규칙:
 * - 어르신 본인: 항상 접근 가능
 * - 관리자(ADMIN): 항상 접근 가능
 * - 상담사(COUNSELOR): 담당 어르신에 대해 접근 가능
 * - 보호자(GUARDIAN): 동의서 + 가족관계증명서 제출 후 관리자 승인을 받아야 접근 가능
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ElderlyService {

    private final ElderlyRepository elderlyRepo;
    private final HealthInfoRepository healthRepo;
    private final UserRepository userRepo;
    private final AdministrativeDivisionRepository divisionRepository;
    private final AccessRequestRepository accessRequestRepo;
    private final GuardianElderlyRepository guardianElderlyRepo;
    private final AssignmentRepository assignmentRepo;

    @Transactional
    public ElderlySummaryResponse createElderly(ElderlyCreateRequest req) {
        User user = userRepo.findById(req.userId())
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        if (user.getStatus() == UserStatus.DELETED) throw new IllegalStateException("USER_DELETED");
        if (user.getRole() != Role.ELDERLY) throw new IllegalStateException("ROLE_NOT_ELDERLY");

        if (elderlyRepo.existsById(user.getId())) {
            throw new IllegalStateException("ELDERLY_ALREADY_EXISTS");
        }

        // 행정구역 존재 여부 확인
        AdministrativeDivision division = divisionRepository.findById(req.admCode())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 행정구역 코드입니다: " + req.admCode()));

        Elderly elderly = Elderly.create(user, division, req.birthDate(), req.gender());
        elderly.updateAddress(req.addressLine1(), req.addressLine2(), req.zipcode());

        Elderly saved = elderlyRepo.save(elderly);

        log.info("어르신 등록 완료 - userId: {}, admCode: {}", user.getId(), req.admCode());

        return ElderlySummaryResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ElderlySummaryResponse getSummary(Long elderlyUserId) {
        Elderly elderly = elderlyRepo.findWithUserById(elderlyUserId)
                .orElseThrow(() -> new IllegalArgumentException("ELDERLY_NOT_FOUND"));
        return ElderlySummaryResponse.from(elderly);
    }

    @Transactional
    public void updateAddress(Long elderlyUserId, String line1, String line2, String zipcode) {
        Elderly elderly = elderlyRepo.findById(elderlyUserId)
                .orElseThrow(() -> new IllegalArgumentException("ELDERLY_NOT_FOUND"));
        elderly.updateAddress(line1, line2, zipcode);
    }

    /**
     * 어르신 행정구역 변경
     */
    @Transactional
    public void changeAdministrativeDivision(Long elderlyUserId, Long admCode) {
        Elderly elderly = elderlyRepo.findById(elderlyUserId)
                .orElseThrow(() -> new IllegalArgumentException("ELDERLY_NOT_FOUND"));

        AdministrativeDivision division = divisionRepository.findById(admCode)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 행정구역 코드입니다: " + admCode));

        elderly.changeAdministrativeDivision(division);
        log.info("어르신 행정구역 변경 완료 - userId: {}, newAdmCode: {}", elderlyUserId, admCode);
    }

    /**
     * 건강정보 조회 (민감정보 - 권한 체크 필수)
     */
    @Transactional(readOnly = true)
    public HealthInfoResponse getHealthInfo(Long requesterUserId, Long elderlyUserId) {
        // 민감정보 접근 권한 확인
        assertCanReadHealthInfo(requesterUserId, elderlyUserId);

        ElderlyHealthInfo hi = healthRepo.findById(elderlyUserId)
                .orElseThrow(() -> new IllegalArgumentException("HEALTH_INFO_NOT_FOUND"));

        return HealthInfoResponse.from(hi);
    }

    /**
     * 건강정보 등록/수정 (민감정보 - 권한 체크 필수)
     */
    @Transactional
    public HealthInfoResponse upsertHealthInfo(Long requesterUserId, Long elderlyUserId, HealthInfoUpdateRequest req) {
        // 민감정보 쓰기 권한 확인
        assertCanWriteHealthInfo(requesterUserId, elderlyUserId);

        Elderly elderly = elderlyRepo.findById(elderlyUserId)
                .orElseThrow(() -> new IllegalArgumentException("ELDERLY_NOT_FOUND"));

        ElderlyHealthInfo hi = healthRepo.findById(elderlyUserId)
                .orElseGet(() -> ElderlyHealthInfo.create(elderly));

        hi.update(req.chronicDiseases(), req.mentalHealthNotes(), req.specialNotes());

        ElderlyHealthInfo saved = healthRepo.save(hi);
        return HealthInfoResponse.from(saved);
    }

    // ========== 민감정보 접근 권한 검증 메서드 ==========

    /**
     * 민감정보 읽기 권한 확인
     */
    private void assertCanReadHealthInfo(Long requesterUserId, Long elderlyUserId) {
        // 1. 본인 확인
        if (requesterUserId.equals(elderlyUserId)) {
            log.debug("건강정보 접근 허용 - 본인 (userId: {})", requesterUserId);
            return;
        }

        User requester = userRepo.findById(requesterUserId)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        // 2. 관리자는 항상 접근 가능
        if (requester.getRole() == Role.ADMIN) {
            log.debug("건강정보 접근 허용 - 관리자 (adminId: {})", requesterUserId);
            return;
        }

        // 3. 상담사: 담당 어르신에 대해서만 접근 가능
        if (requester.getRole() == Role.COUNSELOR) {
            validateCounselorAccess(requesterUserId, elderlyUserId);
            return;
        }

        // 4. 보호자: 동의서 + 가족관계증명서 제출 후 관리자 승인 받은 경우만 접근 가능
        if (requester.getRole() == Role.GUARDIAN) {
            validateGuardianAccess(requesterUserId, elderlyUserId, AccessScope.HEALTH_INFO);
            return;
        }

        // 그 외 역할은 접근 불가
        log.warn("건강정보 접근 거부 - 권한 없음 (requesterId: {}, elderlyId: {}, role: {})",
                requesterUserId, elderlyUserId, requester.getRole());
        throw new AccessDeniedException("해당 어르신의 건강정보에 접근할 권한이 없습니다.");
    }

    /**
     * 민감정보 쓰기 권한 확인
     */
    private void assertCanWriteHealthInfo(Long requesterUserId, Long elderlyUserId) {
        User requester = userRepo.findById(requesterUserId)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        // 관리자는 항상 쓰기 가능
        if (requester.getRole() == Role.ADMIN) {
            log.debug("건강정보 쓰기 권한 허용 - 관리자 (adminId: {})", requesterUserId);
            return;
        }

        // 상담사: 담당 어르신에 대해서만 쓰기 가능
        if (requester.getRole() == Role.COUNSELOR) {
            validateCounselorAccess(requesterUserId, elderlyUserId);
            log.debug("건강정보 쓰기 권한 허용 - 담당 상담사 (counselorId: {})", requesterUserId);
            return;
        }

        // 그 외 (보호자 포함)는 쓰기 불가
        log.warn("건강정보 쓰기 거부 - 권한 없음 (requesterId: {}, elderlyId: {}, role: {})",
                requesterUserId, elderlyUserId, requester.getRole());
        throw new AccessDeniedException("건강정보를 수정할 권한이 없습니다. 관리자 또는 담당 상담사만 수정할 수 있습니다.");
    }

    /**
     * 상담사의 담당 어르신 접근 권한 확인
     */
    private void validateCounselorAccess(Long counselorUserId, Long elderlyUserId) {
        boolean isAssigned = assignmentRepo.existsByCounselorIdAndElderlyIdAndStatusActive(
                counselorUserId, elderlyUserId);

        if (!isAssigned) {
            log.warn("상담사 접근 거부 - 담당 어르신 아님 (counselorId: {}, elderlyId: {})",
                    counselorUserId, elderlyUserId);
            throw new AccessDeniedException("담당하지 않는 어르신의 정보에 접근할 수 없습니다.");
        }

        log.debug("상담사 접근 권한 검증 완료 - counselorId: {}, elderlyId: {}", counselorUserId, elderlyUserId);
    }

    /**
     * 보호자의 민감정보 접근 권한 확인
     */
    private void validateGuardianAccess(Long guardianUserId, Long elderlyUserId, AccessScope scope) {
        // 1. 보호자-어르신 관계 확인
        boolean isGuardian = guardianElderlyRepo.findByGuardianId(guardianUserId)
                .map(ge -> ge.getElderly().getId().equals(elderlyUserId))
                .orElse(false);

        if (!isGuardian) {
            log.warn("보호자 접근 거부 - 보호자-어르신 관계 없음 (guardianId: {}, elderlyId: {})",
                    guardianUserId, elderlyUserId);
            throw new AccessDeniedException("해당 어르신의 보호자가 아닙니다.");
        }

        // 2. 승인된 접근 권한 확인
        boolean hasValidAccess = accessRequestRepo.hasValidAccess(
                guardianUserId,
                elderlyUserId,
                scope,
                LocalDateTime.now()
        );

        if (!hasValidAccess) {
            log.warn("보호자 접근 거부 - 승인된 접근 권한 없음 (guardianId: {}, elderlyId: {}, scope: {})",
                    guardianUserId, elderlyUserId, scope);
            throw new AccessDeniedException(
                    "어르신의 민감정보 열람 권한이 없습니다. " +
                            "동의서와 가족관계증명서를 제출하고 관리자의 승인을 받아주세요."
            );
        }

        log.debug("보호자 접근 허용 - guardianId: {}, elderlyId: {}, scope: {}",
                guardianUserId, elderlyUserId, scope);
    }
}