package com.aicc.silverlink.domain.consent.service;

import com.aicc.silverlink.domain.admin.entity.Admin;
import com.aicc.silverlink.domain.admin.repository.AdminRepository;
import com.aicc.silverlink.domain.consent.dto.AccessRequestDto.*;
import com.aicc.silverlink.domain.consent.entity.AccessRequest;
import com.aicc.silverlink.domain.consent.entity.AccessRequest.AccessRequestStatus;
import com.aicc.silverlink.domain.consent.entity.AccessRequest.AccessScope;
import com.aicc.silverlink.domain.consent.repository.AccessRequestRepository;
import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.domain.elderly.repository.ElderlyRepository;
import com.aicc.silverlink.domain.guardian.entity.Guardian;
import com.aicc.silverlink.domain.guardian.entity.GuardianElderly;
import com.aicc.silverlink.domain.guardian.entity.RelationType;
import com.aicc.silverlink.domain.guardian.repository.GuardianElderlyRepository;
import com.aicc.silverlink.domain.notification.service.NotificationService;
import com.aicc.silverlink.domain.system.entity.AdministrativeDivision;
import com.aicc.silverlink.domain.system.entity.AdministrativeDivision.DivisionLevel;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.entity.UserStatus;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccessRequestService 테스트")
class AccessRequestServiceTest {

    @InjectMocks
    private AccessRequestService accessRequestService;

    @Mock
    private AccessRequestRepository accessRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ElderlyRepository elderlyRepository;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private GuardianElderlyRepository guardianElderlyRepository;

    @Mock
    private NotificationService notificationService;

    // 테스트 픽스처
    private User guardianUser;
    private User elderlyUser;
    private User adminUser;

    private AdministrativeDivision yeoksamDong; // [추가] 행정구역 픽스처

    private Guardian guardian;
    private Elderly elderly;
    private Admin admin;
    private GuardianElderly guardianElderly;

    private AccessRequest pendingRequest;
    private AccessRequest approvedRequest;

    @BeforeEach
    void setUp() {
        // 1. 행정구역 (AdministrativeDivision) 픽스처 생성
        yeoksamDong = AdministrativeDivision.builder()
                .admCode(1168010100L)
                .sidoName("서울특별시")
                .sigunguName("강남구")
                .dongName("역삼1동")
                .level(DivisionLevel.DONG)
                .build();

        // 2. 사용자 (User) 생성
        guardianUser = User.builder()
                .loginId("guardian1")
                .passwordHash("encoded")
                .name("김보호")
                .phone("01012345678")
                .email("guardian@test.com")
                .role(Role.GUARDIAN)
                .status(UserStatus.ACTIVE)
                .build();
        setId(guardianUser, 1L);

        elderlyUser = User.builder()
                .loginId("elderly1")
                .passwordHash("encoded")
                .name("박어르신")
                .phone("01087654321")
                .role(Role.ELDERLY)
                .status(UserStatus.ACTIVE)
                .build();
        setId(elderlyUser, 2L);

        adminUser = User.builder()
                .loginId("admin1")
                .passwordHash("encoded")
                .name("이관리")
                .phone("01011112222")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();
        setId(adminUser, 3L);

        // 3. Elderly 엔티티 생성 (수정: AdministrativeDivision 객체 주입)
        // 주의: Elderly 엔티티 코드가 제공되지 않았으나, AccessRequestDto를 보면 객체를 참조하는 것으로 추정됩니다.
        // 만약 Elderly가 String 코드를 쓴다면 원복해야 하지만, Admin과의 일관성을 위해 객체로 가정합니다.
        // 여기서는 에러 방지를 위해 리플렉션이나 모킹 대신, 일반적인 빌더 패턴을 사용하되
        // 실제 Elderly 엔티티 정의에 맞게 필드명을 확인해야 합니다.
        // (AccessRequestDto.ElderlyInfo.from 메서드를 보면
        // elderly.getAdministrativeDivision() 호출함)
        elderly = Elderly.builder()
                .user(elderlyUser)
                .administrativeDivision(yeoksamDong) // [수정] String -> Object
                .birthDate(LocalDate.of(1940, 5, 15))
                .gender(Elderly.Gender.M)
                .build();
        setElderlyId(elderly, 2L);

        // 4. Guardian 엔티티 생성
        guardian = Guardian.builder()
                .user(guardianUser)
                .addressLine1("서울시 강남구")
                .build();

        // 5. Admin 엔티티 생성 (수정: AdministrativeDivision 객체 주입)
        // Admin.java 정의에 따르면 admCode(Long)가 아니라 administrativeDivision(Entity)를 받습니다.
        admin = Admin.builder()
                .user(adminUser)
                .administrativeDivision(yeoksamDong) // [수정] admCode -> administrativeDivision
                .adminLevel(Admin.AdminLevel.DISTRICT)
                .build();

        // 6. GuardianElderly 관계 생성
        guardianElderly = GuardianElderly.builder()
                .guardian(guardian)
                .elderly(elderly)
                .relationType(RelationType.CHILD)
                .build();

        // 7. AccessRequest (대기 중)
        pendingRequest = AccessRequest.builder()
                .requester(guardianUser)
                .elderly(elderly)
                .scope(AccessScope.HEALTH_INFO)
                .status(AccessRequestStatus.PENDING)
                .documentVerified(false)
                .build();
        setAccessRequestId(pendingRequest, 100L);

        // 8. AccessRequest (승인됨)
        approvedRequest = AccessRequest.builder()
                .requester(guardianUser)
                .elderly(elderly)
                .scope(AccessScope.HEALTH_INFO)
                .status(AccessRequestStatus.APPROVED)
                .documentVerified(true)
                .expiresAt(LocalDateTime.now().plusYears(1))
                .build();
        setAccessRequestId(approvedRequest, 101L);
    }

    // ========== 보호자 - 접근 권한 요청 ==========

    @Nested
    @DisplayName("접근 권한 요청 생성")
    class CreateAccessRequestTest {

        @Test
        @DisplayName("성공 - 보호자가 정상적으로 접근 권한 요청")
        void createAccessRequest_Success() {
            // given
            CreateRequest request = new CreateRequest(2L, AccessScope.HEALTH_INFO);

            given(userRepository.findById(1L)).willReturn(Optional.of(guardianUser));
            given(elderlyRepository.findById(2L)).willReturn(Optional.of(elderly));
            given(guardianElderlyRepository.findByGuardianId(1L)).willReturn(Optional.of(guardianElderly));
            given(accessRequestRepository.findActiveRequest(1L, 2L, AccessScope.HEALTH_INFO))
                    .willReturn(Optional.empty());
            given(accessRequestRepository.save(any(AccessRequest.class))).willAnswer(invocation -> {
                AccessRequest ar = invocation.getArgument(0);
                setAccessRequestId(ar, 100L);
                return ar;
            });

            // when
            AccessRequestResponse response = accessRequestService.createAccessRequest(1L, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(100L);
            assertThat(response.scope()).isEqualTo("HEALTH_INFO");
            assertThat(response.status()).isEqualTo("PENDING");
            assertThat(response.documentVerified()).isFalse();

            then(accessRequestRepository).should().save(any(AccessRequest.class));
        }

        @Test
        @DisplayName("실패 - 보호자가 아닌 사용자가 요청")
        void createAccessRequest_Fail_NotGuardian() {
            // given
            CreateRequest request = new CreateRequest(2L, AccessScope.HEALTH_INFO);
            User counselorUser = User.builder()
                    .loginId("counselor1")
                    .passwordHash("encoded")
                    .name("최상담")
                    .phone("01033334444")
                    .role(Role.COUNSELOR)
                    .status(UserStatus.ACTIVE)
                    .build();
            setId(counselorUser, 10L);

            given(userRepository.findById(10L)).willReturn(Optional.of(counselorUser));

            // when & then
            assertThatThrownBy(() -> accessRequestService.createAccessRequest(10L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("ONLY_GUARDIAN_CAN_REQUEST");
        }

        @Test
        @DisplayName("실패 - 보호자-어르신 관계가 없는 경우")
        void createAccessRequest_Fail_NoRelation() {
            // given
            CreateRequest request = new CreateRequest(2L, AccessScope.HEALTH_INFO);

            given(userRepository.findById(1L)).willReturn(Optional.of(guardianUser));
            given(elderlyRepository.findById(2L)).willReturn(Optional.of(elderly));
            given(guardianElderlyRepository.findByGuardianId(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> accessRequestService.createAccessRequest(1L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("GUARDIAN_ELDERLY_RELATION_NOT_FOUND");
        }

        @Test
        @DisplayName("실패 - 이미 대기 중인 요청이 있는 경우")
        void createAccessRequest_Fail_AlreadyPending() {
            // given
            CreateRequest request = new CreateRequest(2L, AccessScope.HEALTH_INFO);

            given(userRepository.findById(1L)).willReturn(Optional.of(guardianUser));
            given(elderlyRepository.findById(2L)).willReturn(Optional.of(elderly));
            given(guardianElderlyRepository.findByGuardianId(1L)).willReturn(Optional.of(guardianElderly));
            given(accessRequestRepository.findActiveRequest(1L, 2L, AccessScope.HEALTH_INFO))
                    .willReturn(Optional.of(pendingRequest));

            // when & then
            assertThatThrownBy(() -> accessRequestService.createAccessRequest(1L, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("REQUEST_ALREADY_PENDING");
        }
    }

    // ========== 관리자 - 서류 확인 ==========

    @Nested
    @DisplayName("서류 확인 처리")
    class VerifyDocumentsTest {

        @Test
        @DisplayName("성공 - 관리자가 서류 확인 완료 처리")
        void verifyDocuments_Success() {
            // given
            VerifyDocumentsRequest request = new VerifyDocumentsRequest(100L);

            given(userRepository.findById(3L)).willReturn(Optional.of(adminUser));
            given(accessRequestRepository.findByIdWithDetails(100L)).willReturn(Optional.of(pendingRequest));

            // when
            AccessRequestResponse response = accessRequestService.verifyDocuments(3L, request);

            // then
            assertThat(response.documentVerified()).isTrue();
        }

        @Test
        @DisplayName("실패 - 관리자가 아닌 사용자가 시도")
        void verifyDocuments_Fail_NotAdmin() {
            // given
            VerifyDocumentsRequest request = new VerifyDocumentsRequest(100L);

            given(userRepository.findById(1L)).willReturn(Optional.of(guardianUser));

            // when & then
            assertThatThrownBy(() -> accessRequestService.verifyDocuments(1L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("ADMIN_ONLY");
        }
    }

    // ========== 관리자 - 승인/거절 ==========

    @Nested
    @DisplayName("접근 권한 승인")
    class ApproveRequestTest {

        @Test
        @DisplayName("성공 - 서류 확인된 요청 승인")
        void approveRequest_Success() {
            // given
            pendingRequest.verifyDocuments(); // 서류 확인 완료 상태로 변경
            ApproveRequest request = new ApproveRequest(100L, LocalDateTime.now().plusYears(1), "승인합니다");

            given(userRepository.findById(3L)).willReturn(Optional.of(adminUser));
            given(adminRepository.findByIdWithUser(3L)).willReturn(Optional.of(admin));
            given(accessRequestRepository.findByIdWithDetails(100L)).willReturn(Optional.of(pendingRequest));

            // when
            AccessRequestResponse response = accessRequestService.approveRequest(3L, request);

            // then
            assertThat(response.status()).isEqualTo("APPROVED");
            assertThat(response.accessGranted()).isTrue();
        }

        @Test
        @DisplayName("실패 - 서류 확인 안 된 요청 승인 시도")
        void approveRequest_Fail_DocumentsNotVerified() {
            // given
            ApproveRequest request = new ApproveRequest(100L, null, null);

            given(userRepository.findById(3L)).willReturn(Optional.of(adminUser));
            given(adminRepository.findByIdWithUser(3L)).willReturn(Optional.of(admin));
            given(accessRequestRepository.findByIdWithDetails(100L)).willReturn(Optional.of(pendingRequest));

            // when & then
            assertThatThrownBy(() -> accessRequestService.approveRequest(3L, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("DOCUMENTS_NOT_VERIFIED");
        }
    }

    @Nested
    @DisplayName("접근 권한 거절")
    class RejectRequestTest {

        @Test
        @DisplayName("성공 - 요청 거절")
        void rejectRequest_Success() {
            // given
            RejectRequest request = new RejectRequest(100L, "서류가 불충분합니다.");

            given(userRepository.findById(3L)).willReturn(Optional.of(adminUser));
            given(adminRepository.findByIdWithUser(3L)).willReturn(Optional.of(admin));
            given(accessRequestRepository.findByIdWithDetails(100L)).willReturn(Optional.of(pendingRequest));

            // when
            AccessRequestResponse response = accessRequestService.rejectRequest(3L, request);

            // then
            assertThat(response.status()).isEqualTo("REJECTED");
            assertThat(response.decisionNote()).isEqualTo("서류가 불충분합니다.");
        }
    }

    // ========== 권한 확인 ==========

    @Nested
    @DisplayName("접근 권한 확인")
    class CheckAccessTest {

        @Test
        @DisplayName("성공 - 승인된 권한 확인")
        void hasAccess_True() {
            // given
            given(accessRequestRepository.hasValidAccess(eq(1L), eq(2L), eq(AccessScope.HEALTH_INFO),
                    any(LocalDateTime.class)))
                    .willReturn(true);

            // when
            boolean result = accessRequestService.hasAccess(1L, 2L, AccessScope.HEALTH_INFO);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("실패 - 권한 없음")
        void hasAccess_False() {
            // given
            given(accessRequestRepository.hasValidAccess(eq(1L), eq(2L), eq(AccessScope.HEALTH_INFO),
                    any(LocalDateTime.class)))
                    .willReturn(false);

            // when
            boolean result = accessRequestService.hasAccess(1L, 2L, AccessScope.HEALTH_INFO);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("상세 결과 - 권한 있음")
        void checkAccess_Granted() {
            // given
            given(accessRequestRepository.findValidAccess(eq(1L), eq(2L), eq(AccessScope.HEALTH_INFO),
                    any(LocalDateTime.class)))
                    .willReturn(Optional.of(approvedRequest));

            // when
            AccessCheckResult result = accessRequestService.checkAccess(1L, 2L, AccessScope.HEALTH_INFO);

            // then
            assertThat(result.hasAccess()).isTrue();
            assertThat(result.scope()).isEqualTo("HEALTH_INFO");
        }

        @Test
        @DisplayName("상세 결과 - 권한 없음")
        void checkAccess_Denied() {
            // given
            given(accessRequestRepository.findValidAccess(eq(1L), eq(2L), eq(AccessScope.HEALTH_INFO),
                    any(LocalDateTime.class)))
                    .willReturn(Optional.empty());

            // when
            AccessCheckResult result = accessRequestService.checkAccess(1L, 2L, AccessScope.HEALTH_INFO);

            // then
            assertThat(result.hasAccess()).isFalse();
            assertThat(result.message()).contains("권한이 없습니다");
        }
    }

    // ========== 철회 ==========

    @Nested
    @DisplayName("접근 권한 철회")
    class RevokeAccessTest {

        @Test
        @DisplayName("성공 - 어르신이 승인된 권한 철회")
        void revokeByElderly_Success() {
            // given
            RevokeRequest request = new RevokeRequest(101L, "더 이상 공개하고 싶지 않습니다.");

            given(accessRequestRepository.findByIdWithDetails(101L)).willReturn(Optional.of(approvedRequest));

            // when
            AccessRequestResponse response = accessRequestService.revokeAccessByElderly(2L, request);

            // then
            assertThat(response.status()).isEqualTo("REVOKED");
        }

        @Test
        @DisplayName("실패 - 다른 어르신의 요청 철회 시도")
        void revokeByElderly_Fail_NotYourRequest() {
            // given
            RevokeRequest request = new RevokeRequest(101L, null);

            given(accessRequestRepository.findByIdWithDetails(101L)).willReturn(Optional.of(approvedRequest));

            // when & then
            assertThatThrownBy(() -> accessRequestService.revokeAccessByElderly(999L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("NOT_YOUR_ACCESS_REQUEST");
        }
    }

    // ========== Helper Methods (Reflection으로 ID 설정) ==========

    private void setId(User user, Long id) {
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setElderlyId(Elderly elderly, Long id) {
        try {
            var field = Elderly.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(elderly, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setAccessRequestId(AccessRequest ar, Long id) {
        try {
            var field = AccessRequest.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(ar, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}