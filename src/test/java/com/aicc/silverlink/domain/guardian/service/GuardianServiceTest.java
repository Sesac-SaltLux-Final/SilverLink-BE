package com.aicc.silverlink.domain.guardian.service;

import com.aicc.silverlink.domain.assignment.entity.AssignmentStatus;
import com.aicc.silverlink.domain.assignment.repository.AssignmentRepository;
import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.domain.elderly.repository.ElderlyRepository;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@ExtendWith(MockitoExtension.class)
class GuardianServiceTest {

    @InjectMocks private GuardianService guardianService;
    @Mock private GuardianRepository guardianRepository;
    @Mock private GuardianElderlyRepository guardianElderlyRepository;
    @Mock private UserRepository userRepository;
    @Mock private ElderlyRepository elderlyRepository;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private PasswordEncoder passwordEncoder;

    // --- 헬퍼 메소드: 테스트용 객체 생성 ---

    private User createTestUser(Long id, String name, Role role) {
        User user = User.createLocal("testId", "hash", name, "010-1111-2222", "test@test.com", role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Guardian createTestGuardian(Long id, String name) {
        User user = createTestUser(id, name, Role.GUARDIAN);
        Guardian guardian = Guardian.builder().user(user).build();
        ReflectionTestUtils.setField(guardian, "id", id);
        return guardian;
    }

    @Nested
    @DisplayName("보호자 등록 및 조회 테스트")
    class BasicOperation {

        @Test
        @DisplayName("성공: 보호자 회원가입")
        void register_Success() {
            // given
            GuardianRequest request = GuardianRequest.builder()
                    .loginId("newGuardian").password("rawPass").name("김보호").phone("010-1111-2222").build();

            given(userRepository.existsByLoginId(any())).willReturn(false);
            given(passwordEncoder.encode(any())).willReturn("encodedPass");

            // when
            guardianService.register(request);

            // then
            verify(userRepository, times(1)).save(any(User.class));
            verify(guardianRepository, times(1)).save(any(Guardian.class));
        }

        @Test
        @DisplayName("실패: 이미 존재하는 아이디로 가입 시도")
        void register_Fail_DuplicateId() {
            given(userRepository.existsByLoginId(any())).willReturn(true);

            assertThatThrownBy(() -> guardianService.register(GuardianRequest.builder().loginId("dup").build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이미 사용 중인 아이디");
        }
    }

    @Nested
    @DisplayName("상담사 권한 검증 테스트")
    class CounselorAuthTests {

        @Test
        @DisplayName("성공: 담당 어르신의 보호자일 경우 상세 정보 반환")
        void getGuardianForCounselor_Success() {
            // given
            Long gId = 1L; Long cId = 100L; Long eId = 2L;
            Guardian guardian = createTestGuardian(gId, "보호자A");

            Elderly elderly = Elderly.builder().build();
            ReflectionTestUtils.setField(elderly, "id", eId);

            GuardianElderly relation = GuardianElderly.builder().elderly(elderly).build();

            given(guardianRepository.findByIdWithUser(gId)).willReturn(Optional.of(guardian));
            given(guardianElderlyRepository.findByGuardianId(gId)).willReturn(Optional.of(relation));
            given(assignmentRepository.existsByCounselor_IdAndElderly_IdAndStatus(cId, eId, AssignmentStatus.ACTIVE))
                    .willReturn(true);

            // when
            GuardianResponse result = guardianService.getGuardianForCounselor(gId, cId);

            // then
            assertThat(result.getName()).isEqualTo("보호자A");
            assertThat(result.getId()).isEqualTo(gId);
        }

        @Test
        @DisplayName("실패: 상담사가 담당하지 않는 어르신의 보호자 조회 시 에러")
        void getGuardianForCounselor_Fail_NotAssigned() {
            // given
            Long gId = 1L; Long cId = 100L; Long eId = 999L; // 담당이 아닌 ID
            Guardian guardian = createTestGuardian(gId, "보호자A");

            Elderly elderly = Elderly.builder().build();
            ReflectionTestUtils.setField(elderly, "id", eId);

            GuardianElderly relation = GuardianElderly.builder().elderly(elderly).build();

            given(guardianRepository.findByIdWithUser(gId)).willReturn(Optional.of(guardian));
            given(guardianElderlyRepository.findByGuardianId(gId)).willReturn(Optional.of(relation));
            given(assignmentRepository.existsByCounselor_IdAndElderly_IdAndStatus(cId, eId, AssignmentStatus.ACTIVE))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() -> guardianService.getGuardianForCounselor(gId, cId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("본인이 담당하는 어르신");
        }
    }

    @Nested
    @DisplayName("어르신 연결 테스트")
    class ConnectionTests {

        @Test
        @DisplayName("실패: 이미 다른 보호자와 연결된 어르신은 연결 불가")
        void connectElderly_Fail_AlreadyConnected() {
            // given
            given(guardianElderlyRepository.existsByElderly_Id(any())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> guardianService.connectElderly(1L, 2L, RelationType.CHILD))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이미 다른 보호자가 등록");
        }
    }
}