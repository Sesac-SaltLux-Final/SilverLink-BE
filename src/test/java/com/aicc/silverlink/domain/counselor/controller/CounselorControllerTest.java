package com.aicc.silverlink.domain.counselor.controller;

import com.aicc.silverlink.domain.counselor.dto.CounselorRequest;
import com.aicc.silverlink.domain.counselor.dto.CounselorResponse;
import com.aicc.silverlink.domain.counselor.service.CounselorService;
import com.aicc.silverlink.domain.user.entity.UserStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
class CounselorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CounselorService counselorService;

    // 테스트용 Request 생성
    private CounselorRequest createRequest() {
        return CounselorRequest.builder()
                .loginId("counselor01")
                .password("pass1234")
                .name("김상담")
                .phone("010-1234-5678")
                .email("counselor@test.com")
                .employeeNo("EMP001")
                .department("복지과")
                .admDongCode("1111051500") // 필수값
                .joinedAt(LocalDate.now())
                .build();
    }

    // 테스트용 Response 생성
    private CounselorResponse createResponse() {
        return CounselorResponse.builder()
                .id(1L)
                .loginId("counselor01")
                .name("김상담")
                .phone("010-1234-5678")
                .email("counselor@test.com")
                .employeeNo("EMP001")
                .department("복지과")
                .status(UserStatus.ACTIVE)
                .admDongCode("1111051500")
                .build();
    }

    @Test
    @DisplayName("상담사 등록 성공 - 관리자 권한")
    void register_Success() throws Exception {
        // given
        CounselorRequest request = createRequest();
        CounselorResponse response = createResponse();

        given(counselorService.register(any(CounselorRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/counselors")
                        .with(user("admin").roles("ADMIN")) // ✅ 관리자 권한 필수
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated()) // 201 Created 확인
                .andExpect(jsonPath("$.name").value("김상담"))
                .andExpect(jsonPath("$.employeeNo").value("EMP001"));
    }

    @Test
    @DisplayName("상담사 등록 실패 - 권한 없음 (일반 유저)")
    void register_Fail_Forbidden() throws Exception {
        // given
        CounselorRequest request = createRequest();

        // when & then
        mockMvc.perform(post("/api/counselors")
                        .with(user("user").roles("USER")) // ❌ 일반 유저
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isForbidden()); // 403 Forbidden
    }

    @Test
    @DisplayName("상담사 등록 실패 - 필수값 누락 (Validation Check)")
    void register_Fail_Validation() throws Exception {
        // given
        CounselorRequest invalidRequest = CounselorRequest.builder()
                .loginId("") // ❌ 빈 값 (아이디 필수)
                .password("") // ❌ 빈 값 (비밀번호 필수)
                .name("김상담")
                .build();

        // when & then
        mockMvc.perform(post("/api/counselors")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest()); // 400 Bad Request
    }

    @Test
    @DisplayName("상담사 단건 조회 성공 - 관리자 권한")
    void getCounselor_Success() throws Exception {
        // given
        Long id = 1L;
        CounselorResponse response = createResponse();

        given(counselorService.getCounselor(id)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/counselors/{id}", id)
                        .with(user("admin").roles("ADMIN"))) // ✅ 관리자
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginId").value("counselor01"));
    }

    @Test
    @DisplayName("상담사 전체 조회 성공 - 관리자 권한")
    void getAllCounselors_Success() throws Exception {
        // given
        List<CounselorResponse> responses = List.of(createResponse());

        given(counselorService.getAllCounselors()).willReturn(responses);

        // when & then
        mockMvc.perform(get("/api/counselors")
                        .with(user("admin").roles("ADMIN"))) // ✅ 관리자
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].name").value("김상담"));
    }
}