package com.aicc.silverlink.domain.guardian.service;

import com.aicc.silverlink.domain.assignment.entity.AssignmentStatus; // ✅ 추가
import com.aicc.silverlink.domain.assignment.repository.AssignmentRepository;
import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.domain.elderly.repository.ElderlyRepository;
import com.aicc.silverlink.domain.guardian.dto.GuardianElderlyResponse;
import com.aicc.silverlink.domain.guardian.dto.GuardianRequest;
import com.aicc.silverlink.domain.guardian.dto.GuardianResponse;
import com.aicc.silverlink.domain.guardian.entity.Guardian;
import com.aicc.silverlink.domain.guardian.entity.GuardianElderly;
import com.aicc.silverlink.domain.guardian.entity.RelationType;
import com.aicc.silverlink.domain.guardian.repository.GuardianElderlyRepository;
import com.aicc.silverlink.domain.guardian.repository.GuardianRepository;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class GuardianService {

    private final GuardianRepository guardianRepository;
    private final GuardianElderlyRepository guardianElderlyRepository;
    private final UserRepository userRepository;
    private final ElderlyRepository elderlyRepository;
    private final AssignmentRepository assignmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public GuardianResponse register(GuardianRequest request){
        if (userRepository.existsByLoginId(request.getLoginId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = User.createLocal(
                request.getLoginId(),
                encodedPassword,
                request.getName(),
                request.getPhone(),
                request.getEmail(),
                Role.GUARDIAN
        );
        userRepository.save(user);

        Guardian guardian = Guardian.create(
                user,
                request.getAddressLine1(),
                request.getAddressLine2(),
                request.getZipcode(),
                LocalDateTime.now()
        );
        guardianRepository.save(guardian);
        return GuardianResponse.from(guardian);
    }

    public GuardianResponse getGuardian(Long id){
        Guardian guardian = guardianRepository.findByIdWithUser(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 보호자를 찾을 수 없습니다."));
        return GuardianResponse.from(guardian);
    }

    // [상담사용] 권한 체크가 포함된 상세 조회
    public GuardianResponse getGuardianForCounselor(Long guardianId, Long counselorId) {
        Guardian guardian = guardianRepository.findByIdWithUser(guardianId)
                .orElseThrow(() -> new IllegalArgumentException("해당 보호자를 찾을 수 없습니다."));

        GuardianElderly relation = guardianElderlyRepository.findByGuardianId(guardianId)
                .orElseThrow(() -> new IllegalArgumentException("이 보호자와 연결된 어르신 정보가 없습니다."));

        validateAssignment(counselorId, relation.getElderly().getId());
        return GuardianResponse.from(guardian);
    }

    @Transactional
    public void connectElderly(Long guardianId, Long elderlyId, RelationType relationType){
        if(guardianElderlyRepository.existsByElderly_Id(elderlyId)){
            throw new IllegalArgumentException("이미 다른 보호자가 등록한 어르신입니다.");
        }
        Guardian guardian = guardianRepository.findById(guardianId)
                .orElseThrow(() -> new IllegalArgumentException("보호자를 찾을 수 없습니다."));
        Elderly elderly = elderlyRepository.findById(elderlyId)
                .orElseThrow(() -> new IllegalArgumentException("어르신을 찾을 수 없습니다."));

        GuardianElderly relation = GuardianElderly.create(guardian, elderly, relationType, LocalDateTime.now());
        guardianElderlyRepository.save(relation);
    }

    public List<GuardianResponse> getAllGuardian(){
        return guardianRepository.findAllWithUser().stream()
                .map(GuardianResponse::from)
                .collect(Collectors.toList());
    }

    public GuardianElderlyResponse getElderlyByGuardian(Long guardianId){
        GuardianElderly guardianElderly = guardianElderlyRepository.findByGuardianId(guardianId)
                .orElseThrow(() -> new IllegalArgumentException("보호할 어르신이 없습니다."));
        return GuardianElderlyResponse.from(guardianElderly);
    }

    // [상담사용] 권한 체크가 포함된 연결 정보 조회
    public GuardianElderlyResponse getElderlyByGuardianForCounselor(Long guardianId, Long counselorId) {
        GuardianElderly guardianElderly = guardianElderlyRepository.findByGuardianId(guardianId)
                .orElseThrow(() -> new IllegalArgumentException("보호할 어르신이 없습니다."));

        validateAssignment(counselorId, guardianElderly.getElderly().getId());
        return GuardianElderlyResponse.from(guardianElderly);
    }

    // ✅ 상담사 권한 검증 (ACTIVE 상태인 배정 정보가 있는지 확인)
    private void validateAssignment(Long counselorId, Long elderlyId) {
        boolean isAssigned = assignmentRepository.existsByCounselor_IdAndElderly_IdAndStatus(
                counselorId, elderlyId, AssignmentStatus.ACTIVE);

        if (!isAssigned) {
            log.warn("Access Denied: Counselor {} tried to access unassigned Elderly {}", counselorId, elderlyId);
            throw new IllegalArgumentException("본인이 담당하는 어르신의 보호자 정보만 조회할 수 있습니다.");
        }
    }
}