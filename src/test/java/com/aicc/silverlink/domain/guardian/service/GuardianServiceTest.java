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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GuardianServiceTest {

    @InjectMocks
    private GuardianService guardianService;

    @Mock private GuardianRepository guardianRepository;
    @Mock private UserRepository userRepository;
    @Mock private GuardianElderlyRepository guardianElderlyRepository;
    @Mock private ElderlyRepository elderlyRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("보호자 회원가입 성공")
    void register() {
        // given
        GuardianRequest request = GuardianRequest.builder()
                .loginId("testId")
                .password("1234")
                .name("철수")
                .phone("010-1234-5678")
                .email("test@email.com")
                .addressLine1("서울")
                .build();

        given(userRepository.existsByLoginId("testId")).willReturn(false);
        given(passwordEncoder.encode("1234")).willReturn("encodedPW");

        // save 호출 시 들어온 객체를 그대로 리턴하도록 설정 (Mocking)
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User user = invocation.getArgument(0);
            // ID가 생성된 것처럼 세팅 (Reflection or Builder 사용 가정이지만 여기선 객체 그대로 반환)
            return user;
        });
        given(guardianRepository.save(any(Guardian.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        GuardianResponse response = guardianService.register(request);

        // then
        assertThat(response.getName()).isEqualTo("철수");
        verify(userRepository, times(1)).save(any(User.class));
        verify(guardianRepository, times(1)).save(any(Guardian.class));
    }

    @Test
    @DisplayName("보호자 내 정보 조회 성공")
    void getGuardian() {
        // given
        Long guardianId = 1L;
        User user = User.builder().id(guardianId).name("철수").email("test@a.com").build();
        Guardian guardian = Guardian.builder().id(guardianId).user(user).addressLine1("서울").build();

        given(guardianRepository.findByIdWithUser(guardianId)).willReturn(Optional.of(guardian));

        // when
        GuardianResponse response = guardianService.getGuardian(guardianId);

        // then
        assertThat(response.getName()).isEqualTo("철수");
        assertThat(response.getAddressLine1()).isEqualTo("서울");
    }

    @Test
    @DisplayName("어르신 연결 성공")
    void connectElderly() {
        // given
        Long guardianId = 1L;
        Long elderlyId = 2L;

        // 1. 가짜 보호자 생성
        User gUser = User.builder().id(guardianId).name("보호자").build();
        Guardian guardian = Guardian.builder().id(guardianId).user(gUser).build();

        // 2. 가짜 어르신 생성 (Entity 규칙 준수)
        User eUser = User.builder().id(elderlyId).name("어르신").build();
        Elderly elderly = Elderly.builder()
                .id(elderlyId)
                .user(eUser)
                .admDongCode("1111000000")      // 필수값
                .birthDate(LocalDate.of(1950, 1, 1)) // 필수값
                .gender(Elderly.Gender.M)       // 필수값
                .build();

        // 3. Mock 설정
        given(guardianElderlyRepository.existsByElderly_Id(elderlyId)).willReturn(false); // 중복 아님
        given(guardianRepository.findById(guardianId)).willReturn(Optional.of(guardian));
        given(elderlyRepository.findById(elderlyId)).willReturn(Optional.of(elderly));

        // when
        guardianService.connectElderly(guardianId, elderlyId, RelationType.CHILD);

        // then
        verify(guardianElderlyRepository, times(1)).save(any(GuardianElderly.class));
    }

    @Test
    @DisplayName("보호자 전체 목록 조회")
    void getAllGuardian() {
        // given
        User user1 = User.builder().name("보호자1").build();
        Guardian g1 = Guardian.builder().user(user1).build();

        User user2 = User.builder().name("보호자2").build();
        Guardian g2 = Guardian.builder().user(user2).build();

        given(guardianRepository.findAllWithUser()).willReturn(List.of(g1, g2));

        // when
        List<GuardianResponse> result = guardianService.getAllGuardian();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("보호자1");
    }

    @Test
    @DisplayName("내 어르신 찾기 (보호자 -> 어르신)")
    void getElderlyByGuardian() {
        // given
        Long guardianId = 1L;

        // 연결 관계 데이터 생성 (Fetch Join 결과 흉내)
        User gUser = User.builder().name("김철수").phone("010-1111-1111").build();
        Guardian guardian = Guardian.builder().id(guardianId).user(gUser).build();

        User eUser = User.builder().name("이영희").phone("010-2222-2222").build();
        Elderly elderly = Elderly.builder().id(2L).user(eUser).build();

        GuardianElderly relation = GuardianElderly.create(guardian, elderly, RelationType.CHILD, LocalDateTime.now());

        given(guardianElderlyRepository.findByGuardianId(guardianId)).willReturn(Optional.of(relation));

        // when
        GuardianElderlyResponse response = guardianService.getElderlyByGuardian(guardianId);

        // then
        assertThat(response.getGuardianName()).isEqualTo("김철수");
        assertThat(response.getElderlyName()).isEqualTo("이영희");
        assertThat(response.getRelationType()).isEqualTo(RelationType.CHILD);
    }

    @Test
    @DisplayName("어르신의 보호자 찾기 (어르신 -> 보호자)")
    void getGuardianByElderly() {
        // given
        Long elderlyId = 2L;

        // 연결 관계 데이터 생성
        User gUser = User.builder().name("박보호").phone("010-3333-3333").build();
        Guardian guardian = Guardian.builder().id(1L).user(gUser).build();

        User eUser = User.builder().name("최노인").phone("010-4444-4444").build();
        Elderly elderly = Elderly.builder().id(elderlyId).user(eUser).build();

        GuardianElderly relation = GuardianElderly.create(guardian, elderly, RelationType.SPOUSE, LocalDateTime.now());

        given(guardianElderlyRepository.findByElderlyId(elderlyId)).willReturn(Optional.of(relation));

        // when
        GuardianElderlyResponse response = guardianService.getGuardianByElderly(elderlyId);

        // then
        assertThat(response.getGuardianName()).isEqualTo("박보호");
        assertThat(response.getRelationType()).isEqualTo(RelationType.SPOUSE);
    }
}