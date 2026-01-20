package com.aicc.silverlink.domain.admin.controller;

import com.aicc.silverlink.domain.admin.dto.request.AdminCreateRequest;
import com.aicc.silverlink.domain.admin.dto.request.AdminUpdateRequest;
import com.aicc.silverlink.domain.admin.dto.response.AdminResponse;
import com.aicc.silverlink.domain.admin.entity.Admin.AdminLevel;
import com.aicc.silverlink.domain.admin.service.AdminService;
import org.junit.jupiter.api.Disabled;
import tools.jackson.databind.json.JsonMapper; // Jackson 3 변경: ObjectMapper 대신 JsonMapper 사용
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AdminController 테스트
 */
@Disabled("auth 마무리 후 다시 시도할 예정")
@SpringBootTest
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@DisplayName("AdminController 테스트")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper; // Jackson 3 변경: ObjectMapper -> JsonMapper (불변 객체)

    @MockitoBean
    private AdminService adminService;

    @Test
    @DisplayName("관리자 생성 - 성공")
    void createAdmin_Success() throws Exception {
        // given
        AdminCreateRequest request = new AdminCreateRequest(1L, 1168010100L, null);
        AdminResponse response = createMockAdminResponse(1L, AdminLevel.DISTRICT);

        given(adminService.createAdmin(any(AdminCreateRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/admins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.admDongCode").value(1168010100L))
                .andExpect(jsonPath("$.adminLevel").value("DISTRICT"));

        verify(adminService).createAdmin(any(AdminCreateRequest.class));
    }

    @Test
    @DisplayName("관리자 생성 - 유효성 검증 실패 (userId null)")
    void createAdmin_ValidationFail_UserIdNull() throws Exception {
        // given
        AdminCreateRequest request = new AdminCreateRequest(null, 1168010100L, null);

        // when & then
        mockMvc.perform(post("/api/admins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("관리자 조회 - 성공")
    void getAdmin_Success() throws Exception {
        // given
        Long userId = 1L;
        AdminResponse response = createMockAdminResponse(userId, AdminLevel.DISTRICT);

        given(adminService.getAdmin(userId)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/admins/{userId}", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$.adminLevel").value("DISTRICT"));

        verify(adminService).getAdmin(userId);
    }

    @Test
    @DisplayName("전체 관리자 목록 조회 - 성공")
    void getAllAdmins_Success() throws Exception {
        // given
        List<AdminResponse> responses = Arrays.asList(
                createMockAdminResponse(1L, AdminLevel.NATIONAL),
                createMockAdminResponse(2L, AdminLevel.PROVINCIAL),
                createMockAdminResponse(3L, AdminLevel.DISTRICT)
        );

        given(adminService.getAllAdmins()).willReturn(responses);

        // when & then
        mockMvc.perform(get("/api/admins"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].adminLevel").value("NATIONAL"))
                .andExpect(jsonPath("$[1].adminLevel").value("PROVINCIAL"))
                .andExpect(jsonPath("$[2].adminLevel").value("DISTRICT"));

        verify(adminService).getAllAdmins();
    }

    @Test
    @DisplayName("행정동 코드로 관리자 조회 - 성공")
    void getAdminsByAdmDongCode_Success() throws Exception {
        // given
        Long admDongCode = 1168010100L;
        List<AdminResponse> responses = Arrays.asList(
                createMockAdminResponse(1L, AdminLevel.DISTRICT)
        );

        given(adminService.getAdminsByAdmDongCode(admDongCode)).willReturn(responses);

        // when & then
        mockMvc.perform(get("/api/admins")
                        .param("admDongCode", admDongCode.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].admDongCode").value(admDongCode));

        verify(adminService).getAdminsByAdmDongCode(admDongCode);
    }

    @Test
    @DisplayName("레벨별 관리자 조회 - 성공")
    void getAdminsByLevel_Success() throws Exception {
        // given
        AdminLevel level = AdminLevel.PROVINCIAL;
        List<AdminResponse> responses = Arrays.asList(
                createMockAdminResponse(1L, AdminLevel.PROVINCIAL),
                createMockAdminResponse(2L, AdminLevel.PROVINCIAL)
        );

        given(adminService.getAdminsByLevel(level)).willReturn(responses);

        // when & then
        mockMvc.perform(get("/api/admins")
                        .param("level", level.name()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].adminLevel").value("PROVINCIAL"));

        verify(adminService).getAdminsByLevel(level);
    }

    @Test
    @DisplayName("상위 관리자 조회 - 성공")
    void getSupervisors_Success() throws Exception {
        // given
        Long admDongCode = 1168010100L;
        List<AdminResponse> responses = Arrays.asList(
                createMockAdminResponse(1L, AdminLevel.CITY),
                createMockAdminResponse(2L, AdminLevel.PROVINCIAL),
                createMockAdminResponse(3L, AdminLevel.NATIONAL)
        );

        given(adminService.getSupervisors(admDongCode)).willReturn(responses);

        // when & then
        mockMvc.perform(get("/api/admins/supervisors")
                        .param("admDongCode", admDongCode.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].adminLevel").value("CITY"))
                .andExpect(jsonPath("$[1].adminLevel").value("PROVINCIAL"))
                .andExpect(jsonPath("$[2].adminLevel").value("NATIONAL"));

        verify(adminService).getSupervisors(admDongCode);
    }

    @Test
    @DisplayName("하위 관리자 조회 - 성공")
    void getSubordinates_Success() throws Exception {
        // given
        Long userId = 1L;
        List<AdminResponse> responses = Arrays.asList(
                createMockAdminResponse(2L, AdminLevel.DISTRICT),
                createMockAdminResponse(3L, AdminLevel.DISTRICT)
        );

        given(adminService.getSubordinates(userId)).willReturn(responses);

        // when & then
        mockMvc.perform(get("/api/admins/{userId}/subordinates", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].adminLevel").value("DISTRICT"));

        verify(adminService).getSubordinates(userId);
    }

    @Test
    @DisplayName("관리자 권한 확인 - 권한 있음")
    void checkJurisdiction_HasJurisdiction() throws Exception {
        // given
        Long userId = 1L;
        Long targetCode = 1168010100L;

        given(adminService.hasJurisdiction(userId, targetCode)).willReturn(true);

        // when & then
        mockMvc.perform(get("/api/admins/{userId}/jurisdiction", userId)
                        .param("targetCode", targetCode.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(adminService).hasJurisdiction(userId, targetCode);
    }

    @Test
    @DisplayName("관리자 권한 확인 - 권한 없음")
    void checkJurisdiction_NoJurisdiction() throws Exception {
        // given
        Long userId = 1L;
        Long targetCode = 2100000000L; // 다른 시도

        given(adminService.hasJurisdiction(userId, targetCode)).willReturn(false);

        // when & then
        mockMvc.perform(get("/api/admins/{userId}/jurisdiction", userId)
                        .param("targetCode", targetCode.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        verify(adminService).hasJurisdiction(userId, targetCode);
    }

    @Test
    @DisplayName("관리자 정보 수정 - 성공")
    void updateAdmin_Success() throws Exception {
        // given
        Long userId = 1L;
        AdminUpdateRequest request = new AdminUpdateRequest(1168020100L);
        AdminResponse response = AdminResponse.builder()
                .userId(userId)
                .loginId("admin123")
                .name("홍길동")
                .phone("010-1234-5678")
                .email("admin@test.com")
                .status("ACTIVE")
                .admDongCode(1168020100L)
                .adminLevel(AdminLevel.DISTRICT)
                .adminLevelDescription("읍/면/동")
                .createdAt(LocalDateTime.now())
                .build();

        given(adminService.updateAdmin(eq(userId), any(AdminUpdateRequest.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(put("/api/admins/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.admDongCode").value(1168020100L));

        verify(adminService).updateAdmin(eq(userId), any(AdminUpdateRequest.class));
    }

    @Test
    @DisplayName("관리자 삭제 - 성공")
    void deleteAdmin_Success() throws Exception {
        // given
        Long userId = 1L;
        doNothing().when(adminService).deleteAdmin(userId);

        // when & then
        mockMvc.perform(delete("/api/admins/{userId}", userId))
                .andDo(print())
                .andExpect(status().isNoContent());

        verify(adminService).deleteAdmin(userId);
    }

    // Helper method
    private AdminResponse createMockAdminResponse(Long userId, AdminLevel level) {
        return AdminResponse.builder()
                .userId(userId)
                .loginId("admin" + userId)
                .name("홍길동")
                .phone("010-1234-5678")
                .email("admin@test.com")
                .status("ACTIVE")
                .admDongCode(determineCodeByLevel(level))
                .adminLevel(level)
                .adminLevelDescription(level.getDescription())
                .createdAt(LocalDateTime.now())
                .lastLoginAt(LocalDateTime.now())
                .build();
    }

    private Long determineCodeByLevel(AdminLevel level) {
        return switch (level) {
            case NATIONAL -> 0L;
            case PROVINCIAL -> 1100000000L;
            case CITY -> 1168000000L;
            case DISTRICT -> 1168010100L;
        };
    }
}