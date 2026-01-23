package com.aicc.silverlink.domain.guardian.controller;

import com.aicc.silverlink.domain.guardian.dto.GuardianElderlyResponse;
import com.aicc.silverlink.domain.guardian.dto.GuardianRequest;
import com.aicc.silverlink.domain.guardian.dto.GuardianResponse;
import com.aicc.silverlink.domain.guardian.entity.RelationType;
import com.aicc.silverlink.domain.guardian.service.GuardianService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class GuardianControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private GuardianService guardianService;

    // --- 테스트용 픽스처 생성기 ---
    private GuardianResponse createGuardianResponse(Long id, String name) {
        return GuardianResponse.builder()
                .id(id)
                .name(name)
                .email("test@test.com")
                .phone("010-1234-5678")
                .build();
    }

    private GuardianElderlyResponse createElderlyResponse() {
        return GuardianElderlyResponse.builder()
                .guardianId(1L)
                .guardianName("김보호")
                .elderlyId(2L)
                .elderlyName("이노인")
                .relationType(RelationType.CHILD)
                .connectedAt(LocalDateTime.now())
                .build();
    }

    /**
     * [교수님의 팁] @AuthenticationPrincipal Long 에 ID를 주입하기 위한 헬퍼 메소드
     */
    private void mockAuthentication(Long userId, String role) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userId, // Principal에 Long ID 직접 주입
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Nested
    @DisplayName("보호자 및 공통 API 테스트")
    class CommonTests {
        @Test
        @DisplayName("성공: 보호자 회원가입 (201 Created)")
        void signup_Success() throws Exception {
            GuardianRequest request = GuardianRequest.builder().loginId("test01").name("김보호").password("password").build();
            given(guardianService.register(any(GuardianRequest.class))).willReturn(createGuardianResponse(1L, "김보호"));

            mockMvc.perform(post("/api/guardians/signup")
                            .with(csrf()) // POST 요청에는 CSRF 토큰이 필요합니다.
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/guardians/1"));
        }

        @Test
        @DisplayName("성공: 보호자 본인의 내 정보 조회 (200 OK)")
        void getMyInfo_Success() throws Exception {
            // Given
            Long mockUserId = 1L;
            mockAuthentication(mockUserId, "GUARDIAN");
            given(guardianService.getGuardian(mockUserId)).willReturn(createGuardianResponse(mockUserId, "나보호"));

            // When & Then
            mockMvc.perform(get("/api/guardians/me"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("나보호"));
        }
    }

    @Nested
    @DisplayName("상담사(Counselor) 전용 API 테스트")
    class CounselorTests {
        @Test
        @DisplayName("성공: 상담사가 담당 보호자 상세 조회")
        void getGuardianByCounselor_Success() throws Exception {
            Long counselorId = 100L;
            Long guardianId = 1L;
            mockAuthentication(counselorId, "COUNSELOR");

            given(guardianService.getGuardianForCounselor(eq(guardianId), eq(counselorId)))
                    .willReturn(createGuardianResponse(guardianId, "상담담당보호자"));

            mockMvc.perform(get("/api/guardians/counselor/{id}", guardianId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("상담담당보호자"));
        }

        @Test
        @DisplayName("성공: 상담사가 보호자와 연결된 어르신 조회")
        void getElderlyByCounselor_Success() throws Exception {
            Long counselorId = 100L;
            Long guardianId = 1L;
            mockAuthentication(counselorId, "COUNSELOR");

            given(guardianService.getElderlyByGuardianForCounselor(eq(guardianId), eq(counselorId)))
                    .willReturn(createElderlyResponse());

            mockMvc.perform(get("/api/guardians/counselor/{id}/elderly", guardianId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.elderlyName").value("이노인"));
        }
    }

    @Nested
    @DisplayName("관리자(Admin) 전용 API 테스트")
    class AdminTests {
        @Test
        @DisplayName("성공: 관리자가 전체 보호자 목록 조회")
        void getAllGuardians_Success() throws Exception {
            mockAuthentication(999L, "ADMIN");
            given(guardianService.getAllGuardian()).willReturn(List.of(createGuardianResponse(1L, "보호자1")));

            mockMvc.perform(get("/api/guardians"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.size()").value(1));
        }

        @Test
        @DisplayName("성공: 관리자가 어르신-보호자 연결 실행")
        void connectElderly_Success() throws Exception {
            mockAuthentication(999L, "ADMIN");

            mockMvc.perform(post("/api/guardians/1/connect")
                            .param("elderlyId", "2")
                            .param("relationType", "CHILD")
                            .with(csrf()))
                    .andExpect(status().isOk());
        }
    }
}