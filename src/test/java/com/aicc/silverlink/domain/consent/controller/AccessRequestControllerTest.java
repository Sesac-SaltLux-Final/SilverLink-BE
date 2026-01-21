package com.aicc.silverlink.domain.consent.controller;

import com.aicc.silverlink.domain.consent.dto.AccessRequestDto.*;
import com.aicc.silverlink.domain.consent.entity.AccessRequest.AccessScope;
import com.aicc.silverlink.domain.consent.service.AccessRequestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc(addFilters = false)

@DisplayName("AccessRequestController 테스트")
class AccessRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AccessRequestService accessRequestService;

    // ========== 보호자 API 테스트 ==========

    @Nested
    @DisplayName("보호자 API")
    class GuardianApiTest {

        @Test
        @DisplayName("POST /api/access-requests - 접근 권한 요청 생성")
        @WithMockUser(username = "1", roles = "GUARDIAN")
        void createRequest_Success() throws Exception {
            // given
            CreateRequest request = new CreateRequest(2L, AccessScope.HEALTH_INFO);

            AccessRequestResponse response = new AccessRequestResponse(
                    100L,
                    new RequesterInfo(1L, "김보호", "01012345678", "guardian@test.com"),
                    new ElderlyInfo(2L, "박어르신", "01087654321", "1168010100"),
                    "HEALTH_INFO",
                    "건강정보",
                    "PENDING",
                    "대기중",
                    false,
                    null,
                    LocalDateTime.now(),
                    null,
                    null,
                    null,
                    null,
                    false
            );

            given(accessRequestService.createAccessRequest(eq(1L), any(CreateRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post("/api/access-requests")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(100L))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.scope").value("HEALTH_INFO"))
                    .andExpect(jsonPath("$.documentVerified").value(false));
        }

        @Test
        @DisplayName("GET /api/access-requests/my - 내 요청 목록 조회")
        @WithMockUser(username = "1", roles = "GUARDIAN")
        void getMyRequests_Success() throws Exception {
            // given
            List<AccessRequestSummary> responses = List.of(
                    new AccessRequestSummary(100L, "김보호", "박어르신", "HEALTH_INFO", "건강정보",
                            "PENDING", "대기중", false, LocalDateTime.now(), false),
                    new AccessRequestSummary(101L, "김보호", "박어르신", "MEDICATION", "복약정보",
                            "APPROVED", "승인됨", true, LocalDateTime.now().minusDays(10), true)
            );

            given(accessRequestService.getMyRequests(1L)).willReturn(responses);

            // when & then
            mockMvc.perform(get("/api/access-requests/my"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value(100L))
                    .andExpect(jsonPath("$[1].status").value("APPROVED"));
        }

        @Test
        @DisplayName("DELETE /api/access-requests/{id} - 요청 취소")
        @WithMockUser(username = "1", roles = "GUARDIAN")
        void cancelRequest_Success() throws Exception {
            // given
            willDoNothing().given(accessRequestService).cancelRequest(1L, 100L);

            // when & then
            mockMvc.perform(delete("/api/access-requests/100")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("POST /api/access-requests - 관리자가 요청 시 403")
        @WithMockUser(username = "1", roles = "ADMIN")
        void createRequest_Forbidden_ForAdmin() throws Exception {
            // given
            CreateRequest request = new CreateRequest(2L, AccessScope.HEALTH_INFO);

            // when & then
            mockMvc.perform(post("/api/access-requests")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }
    }

    // ========== 관리자 API 테스트 ==========

    @Nested
    @DisplayName("관리자 API")
    class AdminApiTest {

        @Test
        @DisplayName("GET /api/access-requests/pending - 대기 중인 요청 목록")
        @WithMockUser(username = "1", roles = "ADMIN")
        void getPendingRequests_Success() throws Exception {
            // given
            List<AccessRequestSummary> responses = List.of(
                    new AccessRequestSummary(100L, "김보호", "박어르신", "HEALTH_INFO", "건강정보",
                            "PENDING", "대기중", false, LocalDateTime.now(), false),
                    new AccessRequestSummary(102L, "이보호", "최어르신", "MEDICATION", "복약정보",
                            "PENDING", "대기중", true, LocalDateTime.now(), false)
            );

            given(accessRequestService.getPendingRequests()).willReturn(responses);

            // when & then
            mockMvc.perform(get("/api/access-requests/pending"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].status").value("PENDING"))
                    .andExpect(jsonPath("$[1].documentVerified").value(true));
        }

        @Test
        @DisplayName("GET /api/access-requests/pending/stats - 대기 요청 통계")
        @WithMockUser(username = "1", roles = "ADMIN")
        void getPendingStats_Success() throws Exception {
            // given
            PendingRequestStats stats = new PendingRequestStats(10, 3, 7);

            given(accessRequestService.getPendingStats()).willReturn(stats);

            // when & then
            mockMvc.perform(get("/api/access-requests/pending/stats"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalPending").value(10))
                    .andExpect(jsonPath("$.documentVerifiedPending").value(3))
                    .andExpect(jsonPath("$.documentNotVerifiedPending").value(7));
        }

        @Test
        @DisplayName("POST /api/access-requests/{id}/verify-documents - 서류 확인 완료")
        @WithMockUser(username = "1", roles = "ADMIN")
        void verifyDocuments_Success() throws Exception {
            // given
            AccessRequestResponse response = new AccessRequestResponse(
                    100L,
                    new RequesterInfo(4L, "김보호", "01012345678", null),
                    new ElderlyInfo(2L, "박어르신", "01087654321", "1168010100"),
                    "HEALTH_INFO",
                    "건강정보",
                    "PENDING",
                    "대기중",
                    true, // documentVerified = true
                    null,
                    LocalDateTime.now(),
                    null,
                    null,
                    null,
                    null,
                    false
            );

            given(accessRequestService.verifyDocuments(eq(1L), any(VerifyDocumentsRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post("/api/access-requests/100/verify-documents")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.documentVerified").value(true));
        }

        @Test
        @DisplayName("POST /api/access-requests/{id}/approve - 요청 승인")
        @WithMockUser(username = "1", roles = "ADMIN")
        void approveRequest_Success() throws Exception {
            // given
            AccessRequestResponse response = new AccessRequestResponse(
                    100L,
                    new RequesterInfo(4L, "김보호", "01012345678", null),
                    new ElderlyInfo(2L, "박어르신", "01087654321", "1168010100"),
                    "HEALTH_INFO",
                    "건강정보",
                    "APPROVED",
                    "승인됨",
                    true,
                    new ReviewerInfo(1L, "이관리"),
                    LocalDateTime.now().minusHours(1),
                    LocalDateTime.now(),
                    LocalDateTime.now().plusYears(1),
                    null,
                    "승인합니다",
                    true
            );

            given(accessRequestService.approveRequest(eq(1L), any(ApproveRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post("/api/access-requests/100/approve")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APPROVED"))
                    .andExpect(jsonPath("$.accessGranted").value(true))
                    .andExpect(jsonPath("$.reviewer.name").value("이관리"));
        }

        @Test
        @DisplayName("POST /api/access-requests/{id}/reject - 요청 거절")
        @WithMockUser(username = "1", roles = "ADMIN")
        void rejectRequest_Success() throws Exception {
            // given
            RejectRequest request = new RejectRequest(100L, "가족관계증명서가 불충분합니다.");

            AccessRequestResponse response = new AccessRequestResponse(
                    100L,
                    new RequesterInfo(4L, "김보호", "01012345678", null),
                    new ElderlyInfo(2L, "박어르신", "01087654321", "1168010100"),
                    "HEALTH_INFO",
                    "건강정보",
                    "REJECTED",
                    "거절됨",
                    false,
                    new ReviewerInfo(1L, "이관리"),
                    LocalDateTime.now().minusHours(1),
                    LocalDateTime.now(),
                    null,
                    null,
                    "가족관계증명서가 불충분합니다.",
                    false
            );

            given(accessRequestService.rejectRequest(eq(1L), any(RejectRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post("/api/access-requests/100/reject")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REJECTED"))
                    .andExpect(jsonPath("$.decisionNote").value("가족관계증명서가 불충분합니다."));
        }

        @Test
        @DisplayName("GET /api/access-requests/pending - 보호자가 접근 시 403")
        @WithMockUser(username = "1", roles = "GUARDIAN")
        void getPendingRequests_Forbidden_ForGuardian() throws Exception {
            // when & then
            mockMvc.perform(get("/api/access-requests/pending"))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }
    }

    // ========== 어르신 API 테스트 ==========

    @Nested
    @DisplayName("어르신 API")
    class ElderlyApiTest {

        @Test
        @DisplayName("GET /api/access-requests/for-me - 나에 대한 요청 목록")
        @WithMockUser(username = "1", roles = "ELDERLY")
        void getRequestsForMe_Success() throws Exception {
            // given
            List<AccessRequestSummary> responses = List.of(
                    new AccessRequestSummary(100L, "김보호", "박어르신", "HEALTH_INFO", "건강정보",
                            "APPROVED", "승인됨", true, LocalDateTime.now().minusDays(30), true)
            );

            given(accessRequestService.getRequestsForElderly(1L)).willReturn(responses);

            // when & then
            mockMvc.perform(get("/api/access-requests/for-me"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].requesterName").value("김보호"));
        }

        @Test
        @DisplayName("POST /api/access-requests/{id}/revoke-by-elderly - 어르신이 권한 철회")
        @WithMockUser(username = "2", roles = "ELDERLY")
        void revokeAccessByElderly_Success() throws Exception {
            // given
            AccessRequestResponse response = new AccessRequestResponse(
                    100L,
                    new RequesterInfo(4L, "김보호", "01012345678", null),
                    new ElderlyInfo(2L, "박어르신", "01087654321", "1168010100"),
                    "HEALTH_INFO",
                    "건강정보",
                    "REVOKED",
                    "철회됨",
                    true,
                    null,
                    LocalDateTime.now().minusDays(30),
                    LocalDateTime.now().minusDays(29),
                    null,
                    LocalDateTime.now(),
                    "어르신 본인에 의한 철회",
                    false
            );

            given(accessRequestService.revokeAccessByElderly(eq(2L), any(RevokeRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post("/api/access-requests/100/revoke-by-elderly")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REVOKED"));
        }
    }

    // ========== 권한 확인 API 테스트 ==========

    @Nested
    @DisplayName("권한 확인 API")
    class CheckAccessApiTest {

        @Test
        @DisplayName("GET /api/access-requests/check - 권한 확인 (권한 있음)")
        @WithMockUser(username = "4", roles = "GUARDIAN")
        void checkAccess_HasAccess() throws Exception {
            // given
            AccessCheckResult result = new AccessCheckResult(
                    true,
                    "HEALTH_INFO",
                    LocalDateTime.now().minusDays(30),
                    LocalDateTime.now().plusMonths(11),
                    "접근 권한이 있습니다."
            );

            given(accessRequestService.checkAccess(4L, 2L, AccessScope.HEALTH_INFO))
                    .willReturn(result);

            // when & then
            mockMvc.perform(get("/api/access-requests/check")
                            .param("elderlyUserId", "2")
                            .param("scope", "HEALTH_INFO"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasAccess").value(true))
                    .andExpect(jsonPath("$.scope").value("HEALTH_INFO"));
        }

        @Test
        @DisplayName("GET /api/access-requests/check - 권한 확인 (권한 없음)")
        @WithMockUser(username = "4", roles = "GUARDIAN")
        void checkAccess_NoAccess() throws Exception {
            // given
            AccessCheckResult result = AccessCheckResult.denied("접근 권한이 없습니다. 관리자에게 권한을 요청하세요.");

            given(accessRequestService.checkAccess(4L, 2L, AccessScope.HEALTH_INFO))
                    .willReturn(result);

            // when & then
            mockMvc.perform(get("/api/access-requests/check")
                            .param("elderlyUserId", "2")
                            .param("scope", "HEALTH_INFO"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasAccess").value(false))
                    .andExpect(jsonPath("$.message").value("접근 권한이 없습니다. 관리자에게 권한을 요청하세요."));
        }
    }

    // ========== Validation 테스트 ==========

    @Nested
    @DisplayName("Validation 테스트")
    class ValidationTest {

        @Test
        @DisplayName("POST /api/access-requests - elderlyUserId null이면 400")
        @WithMockUser(username = "1", roles = "GUARDIAN")
        void createRequest_Validation_ElderlyUserIdNull() throws Exception {
            // given
            String invalidRequest = "{\"scope\": \"HEALTH_INFO\"}"; // elderlyUserId 누락

            // when & then
            mockMvc.perform(post("/api/access-requests")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /api/access-requests/{id}/reject - 거절 사유 없으면 400")
        @WithMockUser(username = "1", roles = "ADMIN")
        void rejectRequest_Validation_ReasonRequired() throws Exception {
            // given
            String invalidRequest = "{\"accessRequestId\": 100}"; // reason 누락

            // when & then
            mockMvc.perform(post("/api/access-requests/100/reject")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }
}