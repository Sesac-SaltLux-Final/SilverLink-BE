package com.aicc.silverlink.domain.welfare.controller;

import com.aicc.silverlink.domain.welfare.dto.WelfareDetailResponse;
import com.aicc.silverlink.domain.welfare.dto.WelfareListResponse;
import com.aicc.silverlink.domain.welfare.dto.WelfareSearchRequest;
import com.aicc.silverlink.domain.welfare.service.WelfareService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
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
class WelfareControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WelfareService welfareService;

    // 더미 데이터 생성 헬퍼 (DTO 필드명에 맞춰 수정됨)
    private WelfareDetailResponse createMockDetailResponse() {
        return WelfareDetailResponse.builder()
                .id(1L)
                .servNm("노인 돌봄 서비스")       // serviceName -> servNm
                .jurMnofNm("서울시청")            // institution -> jurMnofNm
                .servDgst("혼자 사시는 어르신...") // summary -> servDgst
                .category("돌봄")
                .targetDtlCn("65세 이상 독거노인") // target -> targetDtlCn
                .rprsCtadr("서울시 중구...")      // contact/address -> rprsCtadr
                .servDtlLink("http://welfare.seoul.go.kr") // url -> servDtlLink
                // DTO에 없는 필드(viewCount 등)는 제외했습니다.
                .build();
    }

    private WelfareListResponse createMockListResponse() {
        return WelfareListResponse.builder()
                .id(1L)                       // welfareId -> id
                .servNm("노인 돌봄 서비스")       // serviceName -> servNm
                .jurMnofNm("서울시청")            // institution -> jurMnofNm
                .category("돌봄")
                .servDgst("혼자 사시는 어르신...") // summary -> servDgst
                // applyEnd, viewCount 등 DTO에 없는 필드는 제외
                .build();
    }

    @Test
    @DisplayName("데이터 수동 동기화 성공 - 관리자 권한 필요")
    void manualSync_Success() throws Exception {
        mockMvc.perform(post("/api/welfare/sync/manual")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("데이터 동기화 작업이 시작되었습니다. (로그 확인 요망)"));

        verify(welfareService).syncAllWelfareScheduled();
    }

    @Test
    @DisplayName("복지 서비스 검색 및 목록 조회 성공 - 누구나 접근 가능")
    void searchWelfare_Success() throws Exception {
        // given
        List<WelfareListResponse> list = List.of(createMockListResponse());
        Page<WelfareListResponse> pageResult = new PageImpl<>(list, PageRequest.of(0, 10), 1);

        given(welfareService.searchWelfare(any(WelfareSearchRequest.class), any(Pageable.class)))
                .willReturn(pageResult);

        // when & then
        mockMvc.perform(get("/api/welfare")
                        .param("keyword", "돌봄")
                        .param("page", "0")
                        .param("size", "10")
                        .with(user("user").roles("USER")))
                .andDo(print())
                .andExpect(status().isOk())
                // 👇 [수정] JSON 응답 필드명도 DTO와 일치시킴
                .andExpect(jsonPath("$.content[0].servNm").value("노인 돌봄 서비스"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("복지 서비스 상세 조회 성공 - 누구나 접근 가능")
    void getWelfareDetail_Success() throws Exception {
        // given
        Long welfareId = 1L;
        WelfareDetailResponse response = createMockDetailResponse();

        given(welfareService.getWelfareDetail(welfareId)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/welfare/{welfareId}", welfareId)
                        .with(user("user").roles("USER")))
                .andDo(print())
                .andExpect(status().isOk())
                // 👇 [수정] JSON 응답 필드명도 DTO와 일치시킴
                .andExpect(jsonPath("$.servNm").value("노인 돌봄 서비스"))
                .andExpect(jsonPath("$.jurMnofNm").value("서울시청"));
    }
}