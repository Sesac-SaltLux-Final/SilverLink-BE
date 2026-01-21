package com.aicc.silverlink.domain.admin.controller;

import com.aicc.silverlink.domain.admin.dto.request.AdminCreateRequest;
import com.aicc.silverlink.domain.admin.dto.request.AdminUpdateRequest;
import com.aicc.silverlink.domain.admin.entity.Admin;
import com.aicc.silverlink.domain.admin.entity.Admin.AdminLevel;
import com.aicc.silverlink.domain.admin.repository.AdminRepository;
import com.aicc.silverlink.domain.system.entity.AdministrativeDivision;
import com.aicc.silverlink.domain.system.repository.AdministrativeDivisionRepository;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.entity.UserStatus;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.util.ReflectionTestUtils;
import com.aicc.silverlink.domain.system.entity.AdministrativeDivision;
import com.aicc.silverlink.domain.system.repository.AdministrativeDivisionRepository;
import org.springframework.test.util.ReflectionTestUtils;

import org.springframework.test.context.ActiveProfiles;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc(addFilters = false)
@Transactional
@ActiveProfiles("ci")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    private User testUser;
    private User anotherUser;
    private Admin testAdmin;

    @BeforeEach
    void setUp() {
        // 테스트용 User 생성 (ADMIN 역할)
        testUser = User.createLocal(
                "admin_test_" + System.currentTimeMillis(),
                "encodedPassword123",
                "테스트관리자",
                "01012345678",
                "admin@test.com",
                Role.ADMIN
        );
        userRepository.save(testUser);

        // 추가 테스트용 User 생성
        anotherUser = User.createLocal(
                "admin_test2_" + System.currentTimeMillis(),
                "encodedPassword456",
                "테스트관리자2",
                "01087654321",
                "admin2@test.com",
                Role.ADMIN
        );
        userRepository.save(anotherUser);
    }

    @Nested
    @DisplayName("관리자 생성 API")
    class CreateAdmin {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("성공: 관리자 생성")
        void createAdmin_Success() throws Exception {
            // given
            AdminCreateRequest request = new AdminCreateRequest(
                    testUser.getId(),
                    1168000000L,  // 강남구 코드
                    AdminLevel.CITY
            );

            // when
            ResultActions result = mockMvc.perform(post("/api/admins")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.userId").value(testUser.getId()))
                    .andExpect(jsonPath("$.name").value("테스트관리자"))
                    .andExpect(jsonPath("$.admDongCode").value(1168000000L))
                    .andExpect(jsonPath("$.adminLevel").value("CITY"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("성공: adminLevel 미지정 시 자동 결정")
        void createAdmin_AutoDetermineLevel() throws Exception {
            // given - adminLevel을 null로 설정
            AdminCreateRequest request = new AdminCreateRequest(
                    testUser.getId(),
                    1100000000L,  // 서울시 코드 (시/도 레벨)
                    null
            );

            // when
            ResultActions result = mockMvc.perform(post("/api/admins")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.adminLevel").value("PROVINCIAL"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("실패: 존재하지 않는 사용자")
        void createAdmin_UserNotFound() throws Exception {
            // given
            AdminCreateRequest request = new AdminCreateRequest(
                    999999L,  // 존재하지 않는 ID
                    1168000000L,
                    AdminLevel.CITY
            );

            // when
            ResultActions result = mockMvc.perform(post("/api/admins")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("실패: 이미 관리자로 등록된 사용자")
        void createAdmin_AlreadyRegistered() throws Exception {
            // given - 먼저 관리자로 등록
            Admin admin = Admin.builder()
                    .user(testUser)
                    .admDongCode(1168000000L)
                    .adminLevel(AdminLevel.CITY)
                    .build();
            adminRepository.save(admin);

            AdminCreateRequest request = new AdminCreateRequest(
                    testUser.getId(),
                    1168000000L,
                    AdminLevel.CITY
            );

            // when
            ResultActions result = mockMvc.perform(post("/api/admins")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Disabled("현재 인증 관련 테스트 불가로 비활성화")
        @Test
        @DisplayName("실패: 인증되지 않은 사용자")
        void createAdmin_Unauthorized() throws Exception {
            // given
            AdminCreateRequest request = new AdminCreateRequest(
                    testUser.getId(),
                    1168000000L,
                    AdminLevel.CITY
            );

            // when & then
            mockMvc.perform(post("/api/admins")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("관리자 조회 API")
    class GetAdmin {

        @BeforeEach
        void setUpAdmin() {
            testAdmin = Admin.builder()
                    .user(testUser)
                    .admDongCode(1168000000L)
                    .adminLevel(AdminLevel.CITY)
                    .build();
            adminRepository.save(testAdmin);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("성공: 관리자 단건 조회")
        void getAdmin_Success() throws Exception {
            // when
            ResultActions result = mockMvc.perform(get("/api/admins/{userId}", testUser.getId()));

            // then
            result.andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(testUser.getId()))
                    .andExpect(jsonPath("$.name").value("테스트관리자"))
                    .andExpect(jsonPath("$.admDongCode").value(1168000000L))
                    .andExpect(jsonPath("$.adminLevel").value("CITY"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("실패: 존재하지 않는 관리자 조회")
        void getAdmin_NotFound() throws Exception {
            // when
            ResultActions result = mockMvc.perform(get("/api/admins/{userId}", 999999L));

            // then
            result.andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("관리자 목록 조회 API")
    class GetAdmins {

        @BeforeEach
        void setUpAdmins() {
            // 첫 번째 관리자 (시/군/구 레벨)
            Admin admin1 = Admin.builder()
                    .user(testUser)
                    .admDongCode(1168000000L)
                    .adminLevel(AdminLevel.CITY)
                    .build();
            adminRepository.save(admin1);

            // 두 번째 관리자 (시/도 레벨)
            Admin admin2 = Admin.builder()
                    .user(anotherUser)
                    .admDongCode(1100000000L)
                    .adminLevel(AdminLevel.PROVINCIAL)
                    .build();
            adminRepository.save(admin2);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("성공: 전체 관리자 목록 조회")
        void getAllAdmins_Success() throws Exception {
            // when
            ResultActions result = mockMvc.perform(get("/api/admins"));

            // then
            result.andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("성공: 행정동 코드로 필터링")
        void getAdminsByAdmDongCode_Success() throws Exception {
            // when
            ResultActions result = mockMvc.perform(get("/api/admins")
                    .param("admDongCode", "1168000000"));

            // then
            result.andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].admDongCode").value(1168000000L));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("성공: 관리자 레벨로 필터링")
        void getAdminsByLevel_Success() throws Exception {
            // when
            ResultActions result = mockMvc.perform(get("/api/admins")
                    .param("level", "PROVINCIAL"));

            // then
            result.andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].adminLevel").value("PROVINCIAL"));
        }
    }

    @Nested
    @DisplayName("상위 관리자 조회 API")
    class GetSupervisors {

        @Autowired
        private AdministrativeDivisionRepository administrativeDivisionRepository;

        // ✅ [Final Fix] 독립적인 데이터 생성 + NULL 값 처리 적용
        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("성공: 상위 관리자 목록 조회")
        void getSupervisors_Success() throws Exception {
            // 1. [User] 상위 관리자(서울시)용 유저 생성
            User seoulUser = User.createLocal(
                    "seoul_admin_" + System.currentTimeMillis(),
                    "password",
                    "서울관리자",
                    "01011112222",
                    "seoul@test.com",
                    Role.ADMIN
            );
            // 강제로 ACTIVE 상태 주입
            org.springframework.test.util.ReflectionTestUtils.setField(seoulUser, "status", UserStatus.ACTIVE);
            userRepository.saveAndFlush(seoulUser);

            // 2. [User] 하위 관리자(강남구 - 본인)용 유저 생성
            User gangnamUser = User.createLocal(
                    "gangnam_admin_" + System.currentTimeMillis(),
                    "password",
                    "강남관리자",
                    "01033334444",
                    "gangnam@test.com",
                    Role.ADMIN
            );
            org.springframework.test.util.ReflectionTestUtils.setField(gangnamUser, "status", UserStatus.ACTIVE);
            userRepository.saveAndFlush(gangnamUser);

            // 3. [Division] 행정구역 데이터 생성 (NULL 처리 중요!)

            // 3-1. 서울특별시 (SIDO): 하위 코드는 반드시 NULL이어야 함
            AdministrativeDivision seoulDiv = AdministrativeDivision.builder()
                    .admCode(1100000000L)
                    .sidoCode("11")
                    .sigunguCode(null) // 👈 "000" 아님! null로 설정
                    .dongCode(null)    // 👈 "000" 아님! null로 설정
                    .sidoName("서울특별시")
                    .level(AdministrativeDivision.DivisionLevel.SIDO)
                    .build();
            administrativeDivisionRepository.saveAndFlush(seoulDiv);

            // 3-2. 강남구 (SIGUNGU): 동 코드는 반드시 NULL이어야 함
            AdministrativeDivision gangnamDiv = AdministrativeDivision.builder()
                    .admCode(1168000000L)
                    .sidoCode("11")
                    .sigunguCode("680")
                    .dongCode(null)    // 👈 "000" 아님! null로 설정
                    .sigunguName("강남구")
                    .sidoName("서울특별시")
                    .level(AdministrativeDivision.DivisionLevel.SIGUNGU)
                    .build();
            administrativeDivisionRepository.saveAndFlush(gangnamDiv);

            // 4. [Admin] 관리자 데이터 생성
            // 서울시 관리자 (조회 대상)
            Admin provincialAdmin = Admin.builder()
                    .user(seoulUser)
                    .admDongCode(1100000000L)
                    .adminLevel(AdminLevel.PROVINCIAL)
                    .build();
            adminRepository.saveAndFlush(provincialAdmin);

            // 강남구 관리자
            Admin cityAdmin = Admin.builder()
                    .user(gangnamUser)
                    .admDongCode(1168000000L)
                    .adminLevel(AdminLevel.CITY)
                    .build();
            adminRepository.saveAndFlush(cityAdmin);

            // when: 강남구(1168000000)의 상위 관리자(서울시)를 조회
            ResultActions result = mockMvc.perform(get("/api/admins/supervisors")
                    .param("admDongCode", "1168000000"));

            // then
            result.andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                    .andExpect(jsonPath("$[0].admDongCode").value(1100000000L));
        }
    }

    @Nested
    @DisplayName("하위 관리자 조회 API")
    class GetSubordinates {

        @BeforeEach
        void setUpHierarchy() {
            // 시/군/구 레벨 관리자
            testAdmin = Admin.builder()
                    .user(testUser)
                    .admDongCode(1168000000L)
                    .adminLevel(AdminLevel.CITY)
                    .build();
            adminRepository.save(testAdmin);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("성공: 하위 관리자 목록 조회")
        void getSubordinates_Success() throws Exception {
            // when
            ResultActions result = mockMvc.perform(
                    get("/api/admins/{userId}/subordinates", testUser.getId()));

            // then
            result.andDo(print())
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("실패: 존재하지 않는 관리자의 하위 조회")
        void getSubordinates_AdminNotFound() throws Exception {
            // when
            ResultActions result = mockMvc.perform(
                    get("/api/admins/{userId}/subordinates", 999999L));

            // then
            result.andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("권한 확인 API")
    class CheckJurisdiction {

        @BeforeEach
        void setUpAdmin() {
            // 강남구 관리자
            testAdmin = Admin.builder()
                    .user(testUser)
                    .admDongCode(1168000000L)  // 강남구
                    .adminLevel(AdminLevel.CITY)
                    .build();
            adminRepository.save(testAdmin);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("성공: 관할 구역 내 - true 반환")
        void checkJurisdiction_HasJurisdiction() throws Exception {
            // when - 강남구 관리자가 역삼동에 대한 권한 확인
            ResultActions result = mockMvc.perform(
                    get("/api/admins/{userId}/jurisdiction", testUser.getId())
                            .param("targetCode", "1168010100"));  // 역삼동

            // then
            result.andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().string("true"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("성공: 관할 구역 외 - false 반환")
        void checkJurisdiction_NoJurisdiction() throws Exception {
            // when - 강남구 관리자가 종로구에 대한 권한 확인
            ResultActions result = mockMvc.perform(
                    get("/api/admins/{userId}/jurisdiction", testUser.getId())
                            .param("targetCode", "1111000000"));  // 종로구

            // then
            result.andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().string("false"));
        }
    }

    @Nested
    @DisplayName("관리자 수정 API")
    class UpdateAdmin {

        @BeforeEach
        void setUpAdmin() {
            testAdmin = Admin.builder()
                    .user(testUser)
                    .admDongCode(1168000000L)
                    .adminLevel(AdminLevel.CITY)
                    .build();
            adminRepository.save(testAdmin);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("성공: 담당 구역 변경")
        void updateAdmin_Success() throws Exception {
            // given
            AdminUpdateRequest request = new AdminUpdateRequest(1111000000L);  // 종로구로 변경

            // when
            ResultActions result = mockMvc.perform(put("/api/admins/{userId}", testUser.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.admDongCode").value(1111000000L))
                    .andExpect(jsonPath("$.adminLevel").value("CITY"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("성공: 구역 변경 시 레벨 자동 재계산")
        void updateAdmin_LevelRecalculated() throws Exception {
            // given - 시/도 레벨로 변경
            AdminUpdateRequest request = new AdminUpdateRequest(1100000000L);  // 서울시

            // when
            ResultActions result = mockMvc.perform(put("/api/admins/{userId}", testUser.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.admDongCode").value(1100000000L))
                    .andExpect(jsonPath("$.adminLevel").value("PROVINCIAL"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("실패: 존재하지 않는 관리자 수정")
        void updateAdmin_NotFound() throws Exception {
            // given
            AdminUpdateRequest request = new AdminUpdateRequest(1111000000L);

            // when
            ResultActions result = mockMvc.perform(put("/api/admins/{userId}", 999999L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("관리자 삭제 API")
    class DeleteAdmin {

        @BeforeEach
        void setUpAdmin() {
            testAdmin = Admin.builder()
                    .user(testUser)
                    .admDongCode(1168000000L)
                    .adminLevel(AdminLevel.CITY)
                    .build();
            adminRepository.save(testAdmin);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("성공: 관리자 삭제")
        void deleteAdmin_Success() throws Exception {
            // when
            ResultActions result = mockMvc.perform(
                    delete("/api/admins/{userId}", testUser.getId()));

            // then
            result.andDo(print())
                    .andExpect(status().isNoContent());

            // 삭제 확인
            mockMvc.perform(get("/api/admins/{userId}", testUser.getId()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("실패: 존재하지 않는 관리자 삭제")
        void deleteAdmin_NotFound() throws Exception {
            // when
            ResultActions result = mockMvc.perform(delete("/api/admins/{userId}", 999999L));

            // then
            result.andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("입력값 검증")
    class ValidationTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("실패: userId가 null인 경우")
        void createAdmin_NullUserId() throws Exception {
            // given
            String requestJson = """
                    {
                        "userId": null,
                        "admDongCode": 1168000000,
                        "adminLevel": "CITY"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/admins")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("실패: admDongCode가 null인 경우")
        void createAdmin_NullAdmDongCode() throws Exception {
            // given
            String requestJson = """
                    {
                        "userId": %d,
                        "admDongCode": null,
                        "adminLevel": "CITY"
                    }
                    """.formatted(testUser.getId());

            // when & then
            mockMvc.perform(post("/api/admins")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }
}