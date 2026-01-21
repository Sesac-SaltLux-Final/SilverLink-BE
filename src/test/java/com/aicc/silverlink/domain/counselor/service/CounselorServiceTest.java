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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class) // Mockito 사용 설정
class CounselorServiceTest {

    @InjectMocks
    private CounselorService counselorService; // 가짜 객체들이 주입될 서비스

    @Mock
    private CounselorRepository counselorRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AdministrativeDivisionRepository divisionRepository; // [추가] 필수 의존성

    // 테스트 픽스처
    private AdministrativeDivision division;

    @BeforeEach
    void setUp() {
        // 행정구역 더미 데이터 생성
        division = AdministrativeDivision.builder()
                .admCode(1111051500L)
                .sidoName("서울특별시")
                .sigunguName("종로구")
                .dongName("청운효자동")
                .build();
    }

    // 테스트용 더미 데이터 생성 헬퍼 메서드
    private User createDummyUser() {
        User user = User.createLocal(
                "counselor1", "encodedPw", "김상담", "010-1234-5678", "test@email.com", Role.COUNSELOR
        );
        ReflectionTestUtils.setField(user, "id", 1L); // ID 강제 주입
        return user;
    }

    // [수정] create 메서드 시그니처에 맞게 AdministrativeDivision 전달
    private Counselor createDummyCounselor(User user, AdministrativeDivision division) {
        return Counselor.create(
                user, "2024001", "복지팀", "02-123-4567", LocalDate.now(), division
        );
    }

    @Test
    @DisplayName("상담사 등록 성공")
    void register_success() {
        // given (준비)
        CounselorRequest request = new CounselorRequest(
                "counselor1", "pass1234", "김상담", "test@email.com",
                "010-1234-5678", "2024001", "복지팀", "02-123-4567",
                LocalDate.now(), 1111051500L // [수정] String -> Long
        );

        given(userRepository.existsByLoginId(request.getLoginId())).willReturn(false); // 중복 아님
        // [추가] 행정구역 조회 모킹
        given(divisionRepository.findById(request.getAdmCode())).willReturn(Optional.of(division));
        given(passwordEncoder.encode(request.getPassword())).willReturn("encodedPw");

        // when (실행)
        CounselorResponse response = counselorService.register(request);

        // then (검증)
        assertThat(response).isNotNull();
        assertThat(response.getLoginId()).isEqualTo(request.getLoginId());
        assertThat(response.getName()).isEqualTo(request.getName());
        assertThat(response.getAdmCode()).isEqualTo(1111051500L); // 행정구역 코드 확인

        // 실제로 저장이 호출되었는지 확인
        verify(userRepository, times(1)).save(any(User.class));
        verify(counselorRepository, times(1)).save(any(Counselor.class));
    }

    @Test
    @DisplayName("상담사 등록 실패 - 아이디 중복")
    void register_fail_duplicate_id() {
        // given
        CounselorRequest request = CounselorRequest.builder().loginId("duplicateId").build();
        given(userRepository.existsByLoginId("duplicateId")).willReturn(true); // 중복됨

        // when & then
        assertThatThrownBy(() -> counselorService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 사용 중인 아이디입니다.");

        // 저장이 호출되지 않아야 함
        verify(userRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("상담사 상세 조회 성공")
    void getCounselor_success() {
        // given
        Long counselorId = 1L;
        User user = createDummyUser();
        Counselor counselor = createDummyCounselor(user, division); // [수정] division 전달

        given(counselorRepository.findByIdWithUser(counselorId)).willReturn(Optional.of(counselor));

        // when
        CounselorResponse response = counselorService.getCounselor(counselorId);

        // then
        assertThat(response.getId()).isEqualTo(user.getId());
        assertThat(response.getName()).isEqualTo(user.getName());
        assertThat(response.getDepartment()).isEqualTo(counselor.getDepartment());
        assertThat(response.getSidoName()).isEqualTo("서울특별시"); // 행정구역 정보 확인
    }

    @Test
    @DisplayName("상담사 상세 조회 실패 - 존재하지 않는 ID")
    void getCounselor_fail_not_found() {
        // given
        given(counselorRepository.findByIdWithUser(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> counselorService.getCounselor(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 상담사를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("상담사 목록 조회")
    void getAllCounselors() {
        // given
        User user1 = createDummyUser();
        Counselor c1 = createDummyCounselor(user1, division);

        User user2 = User.createLocal("c2", "pw", "이상담", "010-0000-0000", "e@e.com", Role.COUNSELOR);
        ReflectionTestUtils.setField(user2, "id", 2L);
        Counselor c2 = createDummyCounselor(user2, division);

        given(counselorRepository.findAllWithUser()).willReturn(List.of(c1, c2));

        // when
        List<CounselorResponse> list = counselorService.getAllCounselors();

        // then
        assertThat(list).hasSize(2);
        assertThat(list.get(0).getName()).isEqualTo("김상담");
        assertThat(list.get(1).getName()).isEqualTo("이상담");
    }
}