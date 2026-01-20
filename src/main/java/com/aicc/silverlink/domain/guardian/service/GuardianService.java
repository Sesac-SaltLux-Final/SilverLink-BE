package com.aicc.silverlink.domain.guardian.service;

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
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class GuardianService {

    private final GuardianRepository guardianRepository;
    private final GuardianElderlyRepository guardianElderlyRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final ElderlyRepository elderlyRepository;


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
                .orElseThrow(()->new IllegalArgumentException("해당보호자를 찾을 수 없습니다."));

        return GuardianResponse.from(guardian);
    }
    public void connectElderly(Long guardianId, Long elderlyId, RelationType relationType){
        if(guardianElderlyRepository.existsByElderly_Id(elderlyId)){
            throw new IllegalArgumentException("이미 다른 보호자가 들록한 어르신입니다.");
        }
        Guardian guardian = guardianRepository.findById(guardianId)
                .orElseThrow(()->new IllegalArgumentException("보호자를 찾을 수 없습니다."));
        Elderly elderly = elderlyRepository.findById(elderlyId)
                .orElseThrow(()->new IllegalArgumentException("어르신을 찾을 수 없습니다."));

        GuardianElderly relation = GuardianElderly.create(guardian, elderly, relationType,LocalDateTime.now());

        guardianElderlyRepository.save(relation);
    }

    public List<GuardianResponse> getAllGuardian(){
        return guardianRepository.findAllWithUser().stream()
                .map(GuardianResponse::from)
                .collect(Collectors.toList());

    }
    public GuardianElderlyResponse getElderlyByGuardian(Long guardianId){
        GuardianElderly guardianElderly = guardianElderlyRepository.findByGuardianId(guardianId)
                .orElseThrow(()->new IllegalArgumentException("보호할 어르신이 없습니다."));

        return GuardianElderlyResponse.from(guardianElderly);
    }
    public GuardianElderlyResponse getGuardianByElderly(Long elderlyId){
        GuardianElderly guardianElderly = guardianElderlyRepository.findByElderlyId(elderlyId)
                .orElseThrow(()->new IllegalArgumentException("어르신의 보호자가 없습니다."));
        return GuardianElderlyResponse.from(guardianElderly);
    }


}
