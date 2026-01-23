package com.aicc.silverlink.domain.policy.controller;

import com.aicc.silverlink.domain.admin.entity.Admin;
import com.aicc.silverlink.domain.admin.entity.Admin.AdminLevel;
import com.aicc.silverlink.domain.admin.repository.AdminRepository;
import com.aicc.silverlink.domain.policy.dto.PolicyRequest;
import com.aicc.silverlink.domain.policy.entity.Policy;
import com.aicc.silverlink.domain.policy.entity.PolicyType;
import com.aicc.silverlink.domain.policy.repository.PolicyRepository;
import com.aicc.silverlink.domain.system.entity.AdministrativeDivision;
import com.aicc.silverlink.domain.system.repository.AdministrativeDivisionRepository;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc(addFilters = false)
@Transactional
@ActiveProfiles("test")
class PolicyControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PolicyRepository policyRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AdministrativeDivisionRepository divisionRepository;

    private User adminUser;

    @BeforeEach
    void setUp() {
        // 1. 행정구역 생성
        AdministrativeDivision division = AdministrativeDivision.builder()
                .admCode(1100000000L)
                .sidoCode("11")
                .sidoName("서울특별시")
                .level(AdministrativeDivision.DivisionLevel.SIDO)
                .build();
        divisionRepository.save(division);

        // 2. User 생성
        adminUser = User.createLocal(
                "policy_admin_" + System.currentTimeMillis(),
                "password123",
                "정책관리자",
                "010-1111-2222",
                "policy@test.com",
                Role.ADMIN
        );
        userRepository.save(adminUser);

        // 3. Admin 엔티티 생성 (DB/엔티티에서 삭제한 admDongCode는 제외)
        Admin testAdmin = Admin.builder()
                .user(adminUser)
                .administrativeDivision(division)
                .adminLevel(AdminLevel.NATIONAL)
                .build();

        adminRepository.save(testAdmin);
    }

    private void mockAuthentication(Long userId) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Nested
    @DisplayName("약관 조회 테스트")
    class GetPolicyTests {

        @Test
        @DisplayName("성공: 특정 타입의 최신 약관을 조회한다")
        void getLatestPolicy_Success() throws Exception {
            // 💡 [수정] Policy.create 파라미터에 description(null) 추가
            policyRepository.save(Policy.create(PolicyType.TERMS_OF_SERVICE, "v1.0", "최신 내용", true, null, adminUser));

            mockMvc.perform(get("/api/policies/latest/{type}", PolicyType.TERMS_OF_SERVICE))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.version").value("v1.0"))
                    .andExpect(jsonPath("$.policyName").value(PolicyType.TERMS_OF_SERVICE.getDescription()));
        }

        @Test
        @DisplayName("실패: 등록된 약관이 없는 타입을 조회하면 400 에러를 반환한다")
        void getLatestPolicy_NotFound() throws Exception {
            mockMvc.perform(get("/api/policies/latest/{type}", PolicyType.PRIVACY_POLICY))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("약관 생성 테스트")
    class CreatePolicyTests {

        @Test
        @DisplayName("성공: 관리자가 새로운 약관을 등록한다")
        void createPolicy_Success() throws Exception {
            // given
            mockAuthentication(adminUser.getId());

            PolicyRequest request = new PolicyRequest();
            ReflectionTestUtils.setField(request, "policyType", PolicyType.PRIVACY_POLICY);
            ReflectionTestUtils.setField(request, "version", "v2.0");
            ReflectionTestUtils.setField(request, "content", "새로운 개인정보 처리방침");
            ReflectionTestUtils.setField(request, "isMandatory", true);
            // 💡 [추가] description 필드 테스트 데이터 설정
            ReflectionTestUtils.setField(request, "description", "약관 설명입니다.");

            // when
            ResultActions result = mockMvc.perform(post("/api/policies")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.version").value("v2.0"))
                    .andExpect(jsonPath("$.policyName").value(PolicyType.PRIVACY_POLICY.getDescription()));
        }

        @Test
        @DisplayName("실패: 이미 존재하는 버전으로 등록 시도 시 400 에러를 반환한다")
        void createPolicy_Duplicate() throws Exception {
            // given
            mockAuthentication(adminUser.getId());
            // 💡 [수정] Policy.create 파라미터에 description(null) 추가
            policyRepository.save(Policy.create(PolicyType.TERMS_OF_SERVICE, "v1.0", "내용", true, null, adminUser));

            PolicyRequest request = new PolicyRequest();
            ReflectionTestUtils.setField(request, "policyType", PolicyType.TERMS_OF_SERVICE);
            ReflectionTestUtils.setField(request, "version", "v1.0");
            ReflectionTestUtils.setField(request, "content", "중복 내용");
            ReflectionTestUtils.setField(request, "isMandatory", true);

            // when & then
            mockMvc.perform(post("/api/policies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("입력값 검증 테스트")
    class ValidationTests {

        @Test
        @DisplayName("실패: 필수 파라미터가 누락되면 400 에러를 반환한다")
        void createPolicy_InvalidRequest() throws Exception {
            mockAuthentication(adminUser.getId());
            String json = """
                {
                    "policyType": "TERMS_OF_SERVICE",
                    "content": "내용만 있음",
                    "isMandatory": true
                }
                """;

            mockMvc.perform(post("/api/policies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }
    }
}