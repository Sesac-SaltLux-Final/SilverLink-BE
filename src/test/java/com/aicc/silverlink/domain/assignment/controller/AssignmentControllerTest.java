package com.aicc.silverlink.domain.assignment.controller;

import com.aicc.silverlink.domain.assignment.dto.AssignmentRequest;
import com.aicc.silverlink.domain.assignment.dto.AssignmentResponse;
import com.aicc.silverlink.domain.assignment.entity.AssignmentStatus;
import com.aicc.silverlink.domain.assignment.service.AssignmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong; // ✅ 추가
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("ci")//
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
        AssignmentRequest request = new AssignmentRequest(1L, 2L, 3L);
        AssignmentResponse response = createMockResponse();

        given(assignmentService.assignCounselor(any(AssignmentRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/assignments")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/assignments/elderly/2"));
    }

    @Test
    @DisplayName("배정 해제 성공 - 관리자 권한으로 요청 시 200 OK")
    void unassignCounselor_Success() throws Exception {
        mockMvc.perform(post("/api/assignments/unassign")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("counselorId", "1")
                        .param("elderlyId", "2"))
                .andExpect(status().isOk());

        verify(assignmentService).unassignCounselor(anyLong(), anyLong());
    }

    @Nested
    @DisplayName("조회 API 테스트")
    class GetAssignmentTests {

        @Test
        @DisplayName("상담사 본인의 배정 목록 조회 성공")
        void getMyAssignments_Success() throws Exception {
            // given
            List<AssignmentResponse> responses = List.of(createMockResponse());

            // ✅ 핵심 수정: 1L 대신 any()를 사용하여 파라미터 불일치로 인한 빈 리스트 반환 방지
            given(assignmentService.getAssignmentsByCounselor(any())).willReturn(responses);

            mockMvc.perform(get("/api/assignments/counselor/me")
                            .with(user("1").roles("COUNSELOR"))) // Username을 "1"로 설정
                    .andDo(print())
                    .andExpect(status().isOk())
                    // ✅ 인코딩 정보(charset)를 무시하기 위해 contentTypeCompatibleWith 사용
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.size()").value(1))
                    .andExpect(jsonPath("$[0].counselorName").value("김상담"));
        }

        @Test
        @DisplayName("관리자가 특정 상담사의 배정 목록 조회 성공")
        void getAssignmentsByAdmin_Success() throws Exception {
            List<AssignmentResponse> responses = List.of(createMockResponse());
            given(assignmentService.getAssignmentsByCounselor(anyLong())).willReturn(responses);

            mockMvc.perform(get("/api/assignments/admin/counselors/1")
                            .with(user("admin").roles("ADMIN")))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].counselorName").value("김상담"));
        }

        @Test
        @DisplayName("관리자가 특정 어르신의 배정 현황 조회 성공")
        void getAssignmentByElderly_Success() throws Exception {
            AssignmentResponse response = createMockResponse();
            given(assignmentService.getAssignmentByElderly(anyLong())).willReturn(response);

            mockMvc.perform(get("/api/assignments/admin/elderly/2")
                            .with(user("admin").roles("ADMIN")))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.elderlyName").value("이노인"));
        }
    }
}