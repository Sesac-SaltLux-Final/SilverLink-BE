package com.aicc.silverlink.domain.counselor.service;

import com.aicc.silverlink.domain.counselor.dto.CounselorRequest;
import com.aicc.silverlink.domain.counselor.dto.CounselorResponse;
import com.aicc.silverlink.domain.counselor.entity.Counselor;
import com.aicc.silverlink.domain.counselor.repository.CounselorRepository;
import com.aicc.silverlink.domain.system.entity.AdministrativeDivision;
import com.aicc.silverlink.domain.system.repository.AdministrativeDivisionRepository;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class CounselorService {

    private final CounselorRepository counselorRepository;
    private final UserRepository userRepository;
    private final AdministrativeDivisionRepository divisionRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public CounselorResponse register(CounselorRequest request) {
        if (userRepository.existsByLoginId(request.getLoginId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        // 행정구역 존재 여부 확인
        AdministrativeDivision division = divisionRepository.findById(request.getAdmCode())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 행정구역 코드입니다: " + request.getAdmCode()));

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.createLocal(
                request.getLoginId(),
                encodedPassword,
                request.getName(),
                request.getPhone(),
                request.getEmail(),
                Role.COUNSELOR
        );

        userRepository.save(user);

        Counselor counselor = Counselor.create(
                user,
                request.getEmployeeNo(),
                request.getDepartment(),
                request.getOfficePhone(),
                request.getJoinedAt(),
                division
        );
        counselorRepository.save(counselor);

        log.info("상담사 등록 완료 - userId: {}, admCode: {}", user.getId(), request.getAdmCode());

        return CounselorResponse.from(counselor);
    }

    public CounselorResponse getCounselor(Long id) {
        Counselor counselor = counselorRepository.findByIdWithUser(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 상담사를 찾을 수 없습니다."));
        return CounselorResponse.from(counselor);
    }

    public List<CounselorResponse> getAllCounselors() {
        return counselorRepository.findAllWithUser().stream()
                .map(CounselorResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 행정구역 코드로 상담사 목록 조회
     */
    public List<CounselorResponse> getCounselorsByAdmCode(Long admCode) {
        return counselorRepository.findByAdmCode(admCode).stream()
                .map(CounselorResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 시/도 코드로 상담사 목록 조회
     */
    public List<CounselorResponse> getCounselorsBySido(String sidoCode) {
        return counselorRepository.findBySidoCode(sidoCode).stream()
                .map(CounselorResponse::from)
                .collect(Collectors.toList());
    }
}