package com.aicc.silverlink.domain.counselor.controller;

import com.aicc.silverlink.domain.counselor.dto.CounselorRequest;
import com.aicc.silverlink.domain.counselor.dto.CounselorResponse;
import com.aicc.silverlink.domain.counselor.service.CounselorService;
import com.aicc.silverlink.domain.user.entity.UserStatus;
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
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CounselorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CounselorService counselorService;

    // ✅ 필수값이 누락되지 않도록 헬퍼 메서드 수정
    private CounselorRequest createValidRequest(String loginId, String name) {
        return CounselorRequest.builder()
                .loginId(loginId)
                .password("pass1234!")
                .name(name)
                .phone("010-1234-5678") // 👈 이게 빠져서 에러가 났었습니다!
                .email("test@silverlink.com")
                .employeeNo("EMP001")
                .joinedAt(LocalDate.now())
                .admCode(1111051500L)
                .build();
    }

    private CounselorResponse createResponse(Long id, String name) {
        return CounselorResponse.builder()
                .id(id)
                .loginId("counselor_" + id)
                .name(name)
                .employeeNo("EMP" + id)
                .status(UserStatus.ACTIVE)
                .admCode(1111051500L)
                .build();
    }

    @Nested
    @DisplayName("상담사 등록 API")
    class RegisterTests {
        @Test
        @DisplayName("성공: 모든 필수 값을 입력하면 상담사가 등록된다")
        void register_Success() throws Exception {
            // given
            CounselorRequest request = createValidRequest("new_counselor", "박상담");
            given(counselorService.register(any())).willReturn(createResponse(1L, "박상담"));

            // when & then
            mockMvc.perform(post("/api/counselors")
                            .with(user("admin").roles("ADMIN"))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("박상담"));
        }
    }

    @Nested
    @DisplayName("상담사 조회 API")
    class GetCounselorTests {

        @Test
        @DisplayName("성공: 관리자가 특정 상담사를 ID로 조회한다")
        void getCounselorByAdmin_Success() throws Exception {
            given(counselorService.getCounselor(any())).willReturn(createResponse(1L, "김상담"));

            mockMvc.perform(get("/api/counselors/admin/1")
                            .with(user("admin").roles("ADMIN")))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.name").value("김상담"));
        }

        @Test
        @DisplayName("성공: 상담사 본인이 자신의 정보를 조회한다")
        void getCounselorMe_Success() throws Exception {
            given(counselorService.getCounselor(any())).willReturn(createResponse(10L, "본인상담"));

            mockMvc.perform(get("/api/counselors/me")
                            .with(user("10").roles("COUNSELOR")))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("본인상담"));
        }

        @Test
        @DisplayName("성공: 관리자가 상담사 전체 목록을 조회한다")
        void getAllCounselors_Success() throws Exception {
            given(counselorService.getAllCounselors()).willReturn(List.of(createResponse(1L, "상담1")));

            mockMvc.perform(get("/api/counselors")
                            .with(user("admin").roles("ADMIN")))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.size()").value(1));
        }
    }
}