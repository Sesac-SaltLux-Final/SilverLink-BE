package com.aicc.silverlink.domain.notice.controller;

import com.aicc.silverlink.domain.notice.dto.NoticeResponse;
import com.aicc.silverlink.domain.notice.service.NoticeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//@WebMvcTest(NoticeController.class) // Controller만 로드하여 테스트
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false) //"인증 단계에서 막히니까 아예 인증 단계를 건너뛰고 컨트롤러를 테스트하겠다"
class NoticeControllerTest {

    @Autowired
    private MockMvc mockMvc; // HTTP 요청을 시뮬레이션하는 객체

    @MockitoBean
    private NoticeService noticeService; // 가짜 서비스 객체 주입

//    @Test
//    @DisplayName("내 공지사항 목록 조회 테스트")
//    @WithMockUser(username = "testUser", roles = "USER") // 가짜 사용자 로그인 상태
//    void getMyNotices() throws Exception {
//        // given
//        NoticeResponse response = NoticeResponse.builder()
//                .id(1L)
//                .title("테스트 공지")
//                .content("내용")
//                .build();
//
//        // 페이징 결과 Mocking
//        Page<NoticeResponse> pageResult = new PageImpl<>(List.of(response));
//
//        given(noticeService.getMyNotices(any(), any(Pageable.class)))
//                .willReturn(pageResult);
//
//        // when & then
//        mockMvc.perform(get("/api/notices/my") // 실제 Controller의 URL 확인 필요
//                        .param("page", "0")
//                        .param("size", "10"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.content[0].title").value("테스트 공지"));
//    }

    @Test
    @DisplayName("팝업 공지 조회 테스트")
    @WithMockUser
    void getPopups() throws Exception {
        // given
        List<NoticeResponse> popups = List.of(
                NoticeResponse.builder().id(1L).title("팝업1").isPopup(true).build()
        );

        given(noticeService.getActivePopupsForUser(any()))
                .willReturn(popups);

        // when & then
        mockMvc.perform(get("/api/notices/popups")) // 실제 Controller의 URL 확인 필요
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("팝업1"))
                .andExpect(jsonPath("$[0].popup").value(true)); // 필드명 isPopup -> json에선 popup 일 수 있음 (Lombok 설정 확인)
    }

    @Test
    @DisplayName("공지 상세 조회 테스트")
    @WithMockUser
    void getNoticeDetail() throws Exception {
        // given
        Long noticeId = 1L;
        NoticeResponse response = NoticeResponse.builder().id(noticeId).title("상세 공지").build();

        given(noticeService.getNoticeDetail(eq(noticeId), any()))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/api/notices/{id}", noticeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("상세 공지"));
    }

    @Test
    @DisplayName("공지 읽음 처리 테스트")
    @WithMockUser
    void readNotice() throws Exception {
        // given
        Long noticeId = 1L;
        // readNotice는 void 반환이므로 given 불필요 (필요 시 doNothing 사용)

        // when & then
        mockMvc.perform(post("/api/notices/{id}/read", noticeId)
                        .with(csrf())) // POST 요청 시 CSRF 토큰 필요 (Security 사용 시)
                .andExpect(status().isOk());

        // Verify: 서비스 메서드가 실제로 호출되었는지 확인
        verify(noticeService).readNotice(eq(noticeId), any());
    }
}