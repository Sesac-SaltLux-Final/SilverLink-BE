package com.aicc.silverlink.domain.user.controller;

import com.aicc.silverlink.domain.user.dto.UserRequests;
import com.aicc.silverlink.domain.user.dto.UserResponses;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.UserStatus;
import com.aicc.silverlink.domain.user.service.UserCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

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

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserCommandService userCommandService;

    @Test
    @DisplayName("내 프로필 조회 성공")
    @WithMockUser(username = "1", roles = "USER") // SecurityUtils.currentUserId()가 1L을 반환한다고 가정 (실제 구현에 따라 다를 수 있음)
    void me_Success() throws Exception {
        // given
        UserResponses.MyProfileResponse response = new UserResponses.MyProfileResponse(
                1L, "testUser", Role.ELDERLY, UserStatus.ACTIVE, "홍길동", "01012345678", "test@example.com",
                true, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now()
        );

        given(userCommandService.getMyProfile(any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/users/me")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginId").value("testUser"))
                .andExpect(jsonPath("$.name").value("홍길동"));
    }

    @Test
    @DisplayName("내 프로필 수정 성공")
    @WithMockUser(username = "1", roles = "USER")
    void updateMe_Success() throws Exception {
        // given
        UserRequests.UpdateMyProfileRequest request = new UserRequests.UpdateMyProfileRequest("김철수", "new@example.com");
        UserResponses.MyProfileResponse response = new UserResponses.MyProfileResponse(
                1L, "testUser", Role.ELDERLY, UserStatus.ACTIVE, "김철수", "01012345678", "new@example.com",
                true, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now()
        );

        given(userCommandService.updateMyProfile(any(), any(UserRequests.UpdateMyProfileRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(patch("/users/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("김철수"))
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
                .andExpect(status().isForbidden()); // 403 Forbidden
    }
}
