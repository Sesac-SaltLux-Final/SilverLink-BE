package com.aicc.silverlink.domain.policy.service;

import com.aicc.silverlink.domain.admin.entity.Admin;
import com.aicc.silverlink.domain.admin.repository.AdminRepository;
import com.aicc.silverlink.domain.policy.dto.PolicyRequest;
import com.aicc.silverlink.domain.policy.dto.PolicyResponse;
import com.aicc.silverlink.domain.policy.entity.Policy;
import com.aicc.silverlink.domain.policy.entity.PolicyType;
import com.aicc.silverlink.domain.policy.repository.PolicyRepository;
import com.aicc.silverlink.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    @InjectMocks
    private PolicyService policyService;

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private AdminRepository adminRepository;

    @Test
    @DisplayName("새로운 약관 등록 - 성공")
    void create_Success() {
        // given
        Long adminId = 1L;
        String description = "테스트용 약관 설명입니다.";
        PolicyRequest req = createRequest(PolicyType.TERMS_OF_SERVICE, "v1.0", description);

        User mockUser = User.builder().name("관리자").build();
        // 💡 Admin 엔티티에서 admDongCode가 제거된 상태이므로 빌더에서도 제외합니다.
        Admin mockAdmin = Admin.builder().user(mockUser).build();

        given(policyRepository.existsByPolicyTypeAndVersion(any(), any())).willReturn(false);
        given(adminRepository.findByIdWithUser(adminId)).willReturn(Optional.of(mockAdmin));

        // 💡 Policy.create() 내부에서 createdAt과 updatedAt이 자동 설정됩니다.
        Policy savedPolicy = req.toEntity(mockUser);
        ReflectionTestUtils.setField(savedPolicy, "id", 100L); // 가짜 ID 주입
        given(policyRepository.save(any(Policy.class))).willReturn(savedPolicy);

        // when
        PolicyResponse result = policyService.create(req, adminId);

        // then
        assertThat(result.getVersion()).isEqualTo("v1.0");
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getDescription()).isEqualTo(description);
        assertThat(result.getUpdatedAt()).isNotNull(); // 💡 추가된 필드 검증
        verify(policyRepository).save(any());
    }

    @Test
    @DisplayName("새로운 약관 등록 - 실패 (중복 버전)")
    void create_Fail_DuplicateVersion() {
        // given
        PolicyRequest req = createRequest(PolicyType.TERMS_OF_SERVICE, "v1.0", "설명");
        given(policyRepository.existsByPolicyTypeAndVersion(any(), any())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> policyService.create(req, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 존재하는 정책 버전");
    }

    @Test
    @DisplayName("최신 약관 조회 - 성공")
    void getLatest_Success() {
        // given
        PolicyType type = PolicyType.PRIVACY_POLICY;
        LocalDateTime now = LocalDateTime.now();

        // 💡 PolicyResponse.from()이 사용하는 필드들을 모두 채워줍니다.
        Policy policy = Policy.builder()
                .policyType(type)
                .version("v2.0")
                .content("내용")
                .description("설명")
                .createdAt(now)
                .updatedAt(now)
                .build();
        given(policyRepository.findFirstByPolicyTypeOrderByCreatedAtDesc(type)).willReturn(Optional.of(policy));

        // when
        PolicyResponse result = policyService.getLatest(type);

        // then
        assertThat(result.getVersion()).isEqualTo("v2.0");
        assertThat(result.getPolicyName()).isEqualTo(type.getDescription());
        assertThat(result.getUpdatedAt()).isEqualTo(now);
    }

    // 💡 Helper 메서드 수정: description 파라미터 추가
    private PolicyRequest createRequest(PolicyType type, String version, String description) {
        PolicyRequest req = new PolicyRequest();
        ReflectionTestUtils.setField(req, "policyType", type);
        ReflectionTestUtils.setField(req, "version", version);
        ReflectionTestUtils.setField(req, "content", "테스트 약관 내용");
        ReflectionTestUtils.setField(req, "isMandatory", true);
        ReflectionTestUtils.setField(req, "description", description);
        return req;
    }
}