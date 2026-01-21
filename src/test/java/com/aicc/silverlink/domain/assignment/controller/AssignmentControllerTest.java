package com.aicc.silverlink.domain.assignment.controller;

import com.aicc.silverlink.domain.assignment.dto.AssignmentRequest;
import com.aicc.silverlink.domain.assignment.dto.AssignmentResponse;
import com.aicc.silverlink.domain.assignment.entity.AssignmentStatus;
import com.aicc.silverlink.domain.assignment.service.AssignmentService;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user; // 👈 401 해결사
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
// 👇 [핵심] 님의 환경에 맞는 패키지 경로 사용 (이게 컴파일 성공했던 경로임)
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
class AssignmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AssignmentService assignmentService;

    private AssignmentResponse createMockResponse() {
        return AssignmentResponse.builder()
                .assignmentId(100L)
                .counselorId(1L)
                .counselorName("김상담")
                .elderlyId(2L)
                .elderlyName("이노인")
                .assignedByAdminName("박관리")
                .status(AssignmentStatus.ACTIVE)
                .assignedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("배정 성공 - 관리자 권한으로 요청 시 201 Created 반환")
    void assignCounselor_Success() throws Exception {
        // given
        AssignmentRequest request = new AssignmentRequest(1L, 2L, 3L);
        AssignmentResponse response = createMockResponse();

        given(assignmentService.assignCounselor(any(AssignmentRequest.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/assignments")
                        // 401 방지: 관리자 권한 강력 주입
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/assignments/elderly/2"))
                .andExpect(jsonPath("$.counselorName").value("김상담"));
    }

    @Test
    @DisplayName("배정 실패 - 권한 없는 사용자(일반 유저)가 요청 시 403 Forbidden")
    void assignCounselor_Fail_Forbidden() throws Exception {
        // given
        AssignmentRequest request = new AssignmentRequest(1L, 2L, 3L);

        // when & then
        mockMvc.perform(post("/api/assignments")
                        // 일반 유저 권한 주입 -> ADMIN 필요하므로 403 기대
                        .with(user("user").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("배정 해제 성공 - 관리자 권한으로 요청 시 200 OK")
    void unassignCounselor_Success() throws Exception {
        // when & then
        mockMvc.perform(post("/api/assignments/unassign")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("counselorId", "1")
                        .param("elderlyId", "2"))
                .andDo(print())
                .andExpect(status().isOk());

        verify(assignmentService).unassignCounselor(1L, 2L);
    }

    @Test
    @DisplayName("상담사별 배정 목록 조회 성공 - 상담사 본인 또는 관리자 요청")
    void getAssignmentsByCounselor_Success() throws Exception {
        // given
        List<AssignmentResponse> responses = List.of(createMockResponse());
        given(assignmentService.getAssignmentsByCounselor(1L)).willReturn(responses);

        // when & then
        mockMvc.perform(get("/api/assignments/counselor/1")
                        .with(user("counselor").roles("COUNSELOR")))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1));
    }

    @Test
    @DisplayName("어르신별 담당자 조회 성공")
    void getAssignmentByElderly_Success() throws Exception {
        // given
        AssignmentResponse response = createMockResponse();
        given(assignmentService.getAssignmentByElderly(2L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/assignments/elderly/2")
                        .with(user("user").roles("USER")))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elderlyName").value("이노인"));
    }
}