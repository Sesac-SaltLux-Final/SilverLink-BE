package com.aicc.silverlink.domain.elderly.service;

import com.aicc.silverlink.domain.elderly.dto.request.ElderlyCreateRequest;
import com.aicc.silverlink.domain.elderly.dto.request.HealthInfoUpdateRequest;
import com.aicc.silverlink.domain.elderly.dto.response.ElderlySummaryResponse;
import com.aicc.silverlink.domain.elderly.dto.response.HealthInfoResponse;
import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.domain.elderly.entity.ElderlyHealthInfo;
import com.aicc.silverlink.domain.elderly.repository.ElderlyRepository;
import com.aicc.silverlink.domain.elderly.repository.HealthInfoRepository;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.entity.UserStatus;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ElderlyService {


    private final ElderlyRepository elderlyRepo;
    private final HealthInfoRepository healthRepo;
    private final UserRepository userRepo;

    @Transactional
    public ElderlySummaryResponse createElderly(ElderlyCreateRequest req){
        User user = userRepo.findById(req.userId())
                .orElseThrow(()-> new IllegalArgumentException("USER_NOT_FOUND"));

        if (user.getStatus() == UserStatus.DELETED) throw new IllegalStateException("USER_DELETED");
        if (user.getRole() != Role.ELDERLY) throw new IllegalStateException("ROLE_NOT_ELDERLY");

        if (elderlyRepo.existsById(user.getId())) {
            throw new IllegalStateException("ELDERLY_ALREADY_EXISTS");
        }

        Elderly elderly = Elderly.create(user, req.admDongCode(), req.birthDate(), req.gender());
        elderly.updateAddress(req.addressLine1(), req.addressLine2(), req.zipcode());

        Elderly saved = elderlyRepo.save(elderly);

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

    // 민감정보 권한 체크 필요
    @Transactional(readOnly = true)
    public HealthInfoResponse getHealthInfo(Long requesterUserId, Long elderlyUserId) {
        assertCanReadHealthInfo(requesterUserId, elderlyUserId);

        ElderlyHealthInfo hi = healthRepo.findById(elderlyUserId)
                .orElseThrow(() -> new IllegalArgumentException("HEALTH_INFO_NOT_FOUND"));

        return HealthInfoResponse.from(hi);
    }

    @Transactional
    public HealthInfoResponse upsertHealthInfo(Long requesterUserId, Long elderlyUserId, HealthInfoUpdateRequest req) {
        assertCanWriteHealthInfo(requesterUserId, elderlyUserId);

        Elderly elderly = elderlyRepo.findById(elderlyUserId)
                .orElseThrow(() -> new IllegalArgumentException("ELDERLY_NOT_FOUND"));

        ElderlyHealthInfo hi = healthRepo.findById(elderlyUserId)
                .orElseGet(() -> ElderlyHealthInfo.create(elderly));

        hi.update(req.chronicDiseases(), req.mentalHealthNotes(), req.specialNotes());

        ElderlyHealthInfo saved = healthRepo.save(hi);
        return HealthInfoResponse.from(saved);
    }

    private void assertCanReadHealthInfo(Long requesterUserId, Long elderlyUserId) {
        if (requesterUserId.equals(elderlyUserId)) {
            return;
        }

        User requester = userRepo.findById(requesterUserId)
                .orElseThrow(()->new IllegalArgumentException("USER_NOT_FOUND"));

        if (requester.getRole() == Role.ADMIN) {
            return;
        }

//        switch (requester.getRole()) {
//            case COUNSELOR -> validateCounselorAccess(requesterUserId, elderlyUserId);
//            case GUARDIAN -> validateGuardianReadAccess(requesterUserId, elderlyUserId);
//            default -> throw new AccessDeniedException("접근 권한이 없는 역할입니다.");
//        }
    }

    private void assertCanWriteHealthInfo(Long requesterUserId, Long elderlyUserId) {
        User requester = userRepo.findById(requesterUserId)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        if (requester.getRole() == Role.ADMIN) {
            return;
        }
    }
}
