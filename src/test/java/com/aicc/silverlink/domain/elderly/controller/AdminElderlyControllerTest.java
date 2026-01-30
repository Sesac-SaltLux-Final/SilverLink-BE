package com.aicc.silverlink.domain.elderly.controller;

import com.aicc.silverlink.domain.counselor.dto.CounselorResponse;
import com.aicc.silverlink.domain.elderly.dto.request.ElderlyCreateRequest;
import com.aicc.silverlink.domain.elderly.dto.request.HealthInfoUpdateRequest;
import com.aicc.silverlink.domain.elderly.dto.response.ElderlyAdminDetailResponse;
import com.aicc.silverlink.domain.elderly.dto.response.ElderlySummaryResponse;
import com.aicc.silverlink.domain.elderly.dto.response.HealthInfoResponse;
import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.domain.elderly.service.ElderlyService;
import com.aicc.silverlink.domain.guardian.dto.GuardianResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("ci")
class AdminElderlyControllerTest {

        @Autowired
        private MockMvc mockMvc;
        @Autowired
        private ObjectMapper objectMapper;
        @MockitoBean
        private ElderlyService elderlyService;

        private ElderlySummaryResponse createSummaryResponse(Long id, String name) {
                return new ElderlySummaryResponse(id, name, "01011112222", 11110L, "서울", "종로", "동", "전체주소",
                                LocalDate.of(1950, 1, 1), 75, Elderly.Gender.M, "주소1", "주소2", "123", "김보호");
        }

        @Test
        @DisplayName("성공: 관리자가 어르신 전체 목록을 조회한다")
        void listAll() throws Exception {
                given(elderlyService.getAllElderlyForAdmin()).willReturn(List.of(createSummaryResponse(10L, "이노인")));

                mockMvc.perform(get("/api/admin/elderly")
                                .with(user("1").roles("ADMIN")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.size()").value(1));
        }

        @Test
        @DisplayName("성공: 관리자가 어르신 통합 상세 정보를 조회한다")
        void detail() throws Exception {
                Long eId = 10L;
                ElderlyAdminDetailResponse response = ElderlyAdminDetailResponse.builder()
                                .elderly(createSummaryResponse(eId, "이노인"))
                                .guardian(GuardianResponse.builder().name("김보호").build())
                                .counselor(CounselorResponse.builder().name("박상담").build())
                                .build();

                given(elderlyService.getElderlyDetailForAdmin(eId)).willReturn(response);

                mockMvc.perform(get("/api/admin/elderly/{elderlyUserId}/detail", eId)
                                .with(user("1").roles("ADMIN")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.elderly.name").value("이노인"));
        }

        @Test
        @DisplayName("성공: 관리자가 새로운 어르신을 등록한다")
        void create() throws Exception {
                ElderlyCreateRequest req = new ElderlyCreateRequest(10L, 11110L, LocalDate.of(1950, 1, 1),
                                Elderly.Gender.M,
                                "주소1", "주소2", "123", null, null, null);
                given(elderlyService.createElderly(any())).willReturn(createSummaryResponse(10L, "이노인"));

                mockMvc.perform(post("/api/admin/elderly")
                                .with(user("1").roles("ADMIN"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("성공: 관리자가 어르신의 건강 정보를 등록하거나 수정한다")
        void upsertHealth() throws Exception {
                Long eId = 10L;
                HealthInfoUpdateRequest req = new HealthInfoUpdateRequest("고혈압", "정상", "없음");
                HealthInfoResponse response = new HealthInfoResponse(eId, "고혈압", "정상", "없음", LocalDateTime.now());

                given(elderlyService.upsertHealthInfo(any(), eq(eId), any())).willReturn(response);

                mockMvc.perform(patch("/api/admin/elderly/{elderlyUserId}/health", eId)
                                .with(user("1").roles("ADMIN"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("실패: 관리자 권한이 없는 사용자가 접근하면 403 에러가 발생한다")
        void listAll_Fail_Forbidden() throws Exception {
                mockMvc.perform(get("/api/admin/elderly")
                                .with(user("2").roles("COUNSELOR")))
                                .andExpect(status().isForbidden());
        }
}