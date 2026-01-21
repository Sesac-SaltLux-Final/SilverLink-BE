package com.aicc.silverlink.domain.guardian.controller;

import com.aicc.silverlink.domain.guardian.dto.GuardianElderlyResponse;
import com.aicc.silverlink.domain.guardian.dto.GuardianRequest;
import com.aicc.silverlink.domain.guardian.dto.GuardianResponse;
import com.aicc.silverlink.domain.guardian.entity.RelationType;
import com.aicc.silverlink.domain.guardian.service.GuardianService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
class GuardianControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GuardianService guardianService;

    // 더미 데이터 생성 헬퍼
    private GuardianRequest createRequest() {
        return GuardianRequest.builder()
                .loginId("guardian01")
                .password("pass1234")
                .name("김보호")
                .email("guardian@test.com")
                .phone("010-1234-5678")
                .addressLine1("서울시 강남구")
                .zipcode("12345")
                .build();
    }

    private GuardianResponse createResponse() {
        return GuardianResponse.builder()
                .id(1L)
                .name("김보호")
                .email("guardian@test.com")
                .phone("010-1234-5678")
                .addressLine1("서울시 강남구")
                .zipcode("12345")
                .build();
    }

    private GuardianElderlyResponse createElderlyResponse() {
        return GuardianElderlyResponse.builder()
                .id(10L)
                .guardianId(1L)
                .guardianName("김보호")
                .elderlyId(2L)
                .elderlyName("이노인")
                .relationType(RelationType.CHILD)
                .connectedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("보호자 회원가입 성공 - 201 Created")
    void signup_Success() throws Exception {
        // given
        GuardianRequest request = createRequest();
        GuardianResponse response = createResponse();

        given(guardianService.register(any(GuardianRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/guardians/signup")
                        .with(csrf()) // 회원가입은 인증 없이 가능하지만 CSRF 토큰은 필요
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/guardians/1"))
                .andExpect(jsonPath("$.name").value("김보호"));
    }

    @Test
    @DisplayName("보호자 단건 조회 성공 - 인증된 사용자")
    void getGuardian_Success() throws Exception {
        // given
        Long guardianId = 1L;
        GuardianResponse response = createResponse();

        given(guardianService.getGuardian(guardianId)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/guardians/{id}", guardianId)
                        .with(user("user").roles("USER"))) // 일반 유저 접근 가능
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("김보호"));
    }

    @Test
    @DisplayName("보호자 전체 목록 조회 성공 - 관리자(ADMIN)만 가능")
    void getAllGuardians_Success() throws Exception {
        // given
        List<GuardianResponse> responses = List.of(createResponse());
        given(guardianService.getAllGuardian()).willReturn(responses);

        // when & then
        mockMvc.perform(get("/api/guardians")
                        .with(user("admin").roles("ADMIN"))) // ✅ 관리자 권한
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1));
    }

    @Test
    @DisplayName("보호자 전체 목록 조회 실패 - 일반 유저는 접근 불가 (403)")
    void getAllGuardians_Fail_Forbidden() throws Exception {
        // when & then
        mockMvc.perform(get("/api/guardians")
                        .with(user("user").roles("USER"))) // ❌ 일반 유저
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("보호자-어르신 연결 성공 - 관리자(ADMIN)만 가능")
    void connectElderly_Success() throws Exception {
        // given
        Long guardianId = 1L;
        Long elderlyId = 2L;
        RelationType relationType = RelationType.CHILD;

        // when & then
        mockMvc.perform(post("/api/guardians/{id}/connect", guardianId)
                        .param("elderlyId", String.valueOf(elderlyId))
                        .param("relationType", relationType.name())
                        .with(user("admin").roles("ADMIN")) // ✅ 관리자 권한
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());

        verify(guardianService).connectElderly(eq(guardianId), eq(elderlyId), eq(relationType));
    }

    @Test
    @DisplayName("내 어르신 조회 성공")
    void getMyElderly_Success() throws Exception {
        // given
        Long guardianId = 1L;
        GuardianElderlyResponse response = createElderlyResponse();

        given(guardianService.getElderlyByGuardian(guardianId)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/guardians/{id}/elderly", guardianId)
                        .with(user("guardian").roles("GUARDIAN"))) // 보호자 권한
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elderlyName").value("이노인"));
    }

    @Test
    @DisplayName("어르신의 보호자 조회 성공")
    void getGuardianOfElderly_Success() throws Exception {
        // given
        Long elderlyId = 2L;
        GuardianElderlyResponse response = createElderlyResponse();

        given(guardianService.getGuardianByElderly(elderlyId)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/guardians/find-by-elderly/{id}", elderlyId)
                        .with(user("admin").roles("ADMIN")))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guardianName").value("김보호"));
    }
}