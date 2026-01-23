package com.aicc.silverlink.domain.user.controller;

import com.aicc.silverlink.domain.user.dto.UserRequests;
import com.aicc.silverlink.domain.user.dto.UserResponses;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.UserStatus;
import com.aicc.silverlink.domain.user.service.UserCommandService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false) // 테스트 편의를 위해 필터는 제외하되 로직 집중
class UserControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private UserCommandService userCommandService;

    @Test
    @DisplayName("내 프로필 조회 성공")
    @WithMockUser(username = "1")
    void me_Success() throws Exception {
        // given
        UserResponses.MyProfileResponse response = new UserResponses.MyProfileResponse(
                1L, "testUser", Role.ELDERLY, UserStatus.ACTIVE, "홍길동", "01012345678",
                "test@example.com", true, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());

        given(userCommandService.getMyProfile(any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/users/me")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginId").value("testUser"))
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$.phone").value("01012345678")); // 전화번호 검증 추가
    }

    @Test
    @DisplayName("내 프로필 수정 성공 - 전화번호 필드 포함")
    @WithMockUser(username = "1")
    void updateMe_Success() throws Exception {
        // given
        // 💡 UpdateMyProfileRequest에 전화번호(phone) 파라미터가 추가된 것을 반영
        UserRequests.UpdateMyProfileRequest request = new UserRequests.UpdateMyProfileRequest(
                "김철수", "01099998888", "new@example.com");

        UserResponses.MyProfileResponse response = new UserResponses.MyProfileResponse(
                1L, "testUser", Role.ELDERLY, UserStatus.ACTIVE, "김철수", "01099998888",
                "new@example.com", true, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());

        given(userCommandService.updateMyProfile(any(), any(UserRequests.UpdateMyProfileRequest.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(patch("/users/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("김철수"))
                .andExpect(jsonPath("$.phone").value("01099998888")) // 수정된 전화번호 확인
                .andExpect(jsonPath("$.email").value("new@example.com"));
    }

    @Test
    @DisplayName("회원 상태 변경 성공 - 관리자 권한")
    @WithMockUser(roles = "ADMIN")
    void changeStatus_Success() throws Exception {
        // given
        Long targetUserId = 2L;
        UserRequests.ChangeStatusRequest request = new UserRequests.ChangeStatusRequest("LOCKED");

        // when & then
        mockMvc.perform(patch("/users/{userId}/status", targetUserId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk());

        // 💡 서비스 메서드 호출 시 정확한 파라미터가 전달되었는지 확인
        verify(userCommandService).ChangeStatus(eq(targetUserId), eq(UserStatus.LOCKED));
    }

    @Test
    @DisplayName("회원 상태 변경 실패 - 권한 없음")
    @WithMockUser(roles = "USER")
    void changeStatus_Fail_Forbidden() throws Exception {
        // given
        Long targetUserId = 2L;
        UserRequests.ChangeStatusRequest request = new UserRequests.ChangeStatusRequest("LOCKED");

        // when & then
        mockMvc.perform(patch("/users/{userId}/status", targetUserId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }
}
