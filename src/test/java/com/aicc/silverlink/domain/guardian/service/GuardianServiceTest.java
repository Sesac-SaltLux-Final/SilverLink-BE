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
import com.aicc.silverlink.domain.system.entity.AdministrativeDivision;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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

    // 테스트 픽스처
    private AdministrativeDivision division;

    @BeforeEach
    void setUp() {
        // 행정구역 더미 데이터 (Elderly 생성 시 필요)
        division = AdministrativeDivision.builder()
                .admCode(1111000000L)
                .sidoName("서울특별시")
                .sigunguName("종로구")
                .build();
    }

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

        // save 호출 시 들어온 객체를 그대로 리턴하도록 설정
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 1L); // ID 생성 시뮬레이션
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
        Guardian guardian = Guardian.builder().user(user).addressLine1("서울").build();
        // Guardian의 PK인 userId 매핑
        ReflectionTestUtils.setField(guardian, "id", guardianId);

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
        Guardian guardian = Guardian.builder().user(gUser).build();
        ReflectionTestUtils.setField(guardian, "id", guardianId);

        // 2. 가짜 어르신 생성 (수정: AdministrativeDivision 객체 사용)
        User eUser = User.builder().id(elderlyId).name("어르신").build();
        Elderly elderly = Elderly.builder()
                .user(eUser)
                .administrativeDivision(division) // [수정] String 코드 대신 객체 주입
                .birthDate(LocalDate.of(1950, 1, 1))
                .gender(Elderly.Gender.M)
                .build();
        ReflectionTestUtils.setField(elderly, "id", elderlyId);

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

        // 보호자 생성
        User gUser = User.builder().name("김철수").phone("010-1111-1111").build();
        Guardian guardian = Guardian.builder().user(gUser).build();
        ReflectionTestUtils.setField(guardian, "id", guardianId);

        // 어르신 생성 (필수값 주입)
        User eUser = User.builder().name("이영희").phone("010-2222-2222").build();
        Elderly elderly = Elderly.builder()
                .user(eUser)
                .administrativeDivision(division)
                .build();
        ReflectionTestUtils.setField(elderly, "id", 2L);

        // 관계 생성
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

        // 보호자 생성
        User gUser = User.builder().name("박보호").phone("010-3333-3333").build();
        Guardian guardian = Guardian.builder().user(gUser).build();
        ReflectionTestUtils.setField(guardian, "id", 1L);

        // 어르신 생성
        User eUser = User.builder().name("최노인").phone("010-4444-4444").build();
        Elderly elderly = Elderly.builder()
                .user(eUser)
                .administrativeDivision(division)
                .build();
        ReflectionTestUtils.setField(elderly, "id", elderlyId);

        // 관계 생성
        GuardianElderly relation = GuardianElderly.create(guardian, elderly, RelationType.SPOUSE, LocalDateTime.now());

        given(guardianElderlyRepository.findByElderlyId(elderlyId)).willReturn(Optional.of(relation));

        // when
        GuardianElderlyResponse response = guardianService.getGuardianByElderly(elderlyId);

        // then
        assertThat(response.getGuardianName()).isEqualTo("박보호");
        assertThat(response.getRelationType()).isEqualTo(RelationType.SPOUSE);
    }
}