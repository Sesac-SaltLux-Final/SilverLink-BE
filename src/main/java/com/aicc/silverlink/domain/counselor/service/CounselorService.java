package com.aicc.silverlink.domain.counselor.service;

import com.aicc.silverlink.domain.counselor.dto.CounselorRequest;
import com.aicc.silverlink.domain.counselor.dto.CounselorResponse;
import com.aicc.silverlink.domain.counselor.entity.Counselor;
import com.aicc.silverlink.domain.counselor.repository.CounselorRepository;
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
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public CounselorResponse register(CounselorRequest request) {
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
                Role.COUNSELOR
        );

        userRepository.save(user);

        Counselor counselor = Counselor.create(
                user,
                request.getEmployeeNo(),
                request.getDepartment(),
                request.getOfficePhone(),
                request.getJoinedAt(),
                request.getAdmDongCode()
        );
        counselorRepository.save(counselor);

        return CounselorResponse.from(counselor);

    }
    public CounselorResponse getCounselor(Long id){
        Counselor counselor = counselorRepository.findByIdWithUser(id)
                .orElseThrow(()->new IllegalArgumentException("해당 상담사를 찾을 수 없습니다."));
        return CounselorResponse.from(counselor);
    }

    public List<CounselorResponse> getAllCounselors(){
        return counselorRepository.findAllWithUser().stream()
                .map(CounselorResponse::from)
                .collect(Collectors.toList());
    }

}
