package com.aicc.silverlink.domain.welfare.service;

import com.aicc.silverlink.domain.welfare.dto.*;
import com.aicc.silverlink.domain.welfare.entity.Welfare;
import com.aicc.silverlink.domain.welfare.repository.WelfareRepository;
import com.aicc.silverlink.domain.welfare.service.WelfareService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WelfareServiceTest {

    @InjectMocks
    private WelfareService welfareService;

    @Mock
    private WelfareRepository welfareRepository;

    @Mock
    private ModelMapper modelMapper;

    // [변경 1] RestClient와 그 하위 인터페이스들을 Mock으로 선언
    @Mock
    private RestClient restClient;
    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(welfareService, "serviceKey", "test-api-key");
        ReflectionTestUtils.setField(welfareService, "centralUrl", "http://api.central.go.kr");
        ReflectionTestUtils.setField(welfareService, "localUrl", "http://api.local.go.kr");
        ReflectionTestUtils.setField(welfareService, "centralDetailUrl", "http://api.central.detail.go.kr");
        ReflectionTestUtils.setField(welfareService, "localDetailUrl", "http://api.local.detail.go.kr");
    }

    @Test
    @DisplayName("1. 검색 테스트: 키워드로 검색하면 결과가 반환되어야 한다.")
    void searchWelfareTest() {
        // given
        String keyword = "치매";
        Pageable pageable = PageRequest.of(0, 10);
        WelfareSearchRequest request = new WelfareSearchRequest();
        request.setKeyword(keyword);

        Welfare mockEntity = Welfare.builder().id(1L).servNm("치매 예방 교실").build();
        Page<Welfare> mockPage = new PageImpl<>(List.of(mockEntity));
        WelfareListResponse mockResponse = new WelfareListResponse();
        mockResponse.setServNm("치매 예방 교실");

        given(welfareRepository.searchByKeyword(eq(keyword), any(Pageable.class))).willReturn(mockPage);
        given(modelMapper.map(any(Welfare.class), eq(WelfareListResponse.class))).willReturn(mockResponse);

        // when
        Page<WelfareListResponse> result = welfareService.searchWelfare(request, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getServNm()).isEqualTo("치매 예방 교실");
        verify(welfareRepository, times(1)).searchByKeyword(eq(keyword), any(Pageable.class));
    }

    @Test
    @DisplayName("2. 수집 테스트(성공): '노년' 데이터는 저장되어야 한다.")
    void syncWelfareSaveTest() {
        // given
        WelfareApiDto.CentralItem listItem = new WelfareApiDto.CentralItem();
        listItem.setServId("WLF_TEST_001");
        listItem.setServNm("노인 연금");
        listItem.setLifeArray("노년");

        WelfareApiDto.CentralResponse listResponse = new WelfareApiDto.CentralResponse();
        listResponse.setServList(List.of(listItem));

        WelfareApiDto.DetailItem detailItem = new WelfareApiDto.DetailItem();
        detailItem.setAlwServCn("월 30만원");

        // [변경 2] RestClient 체이닝 Mocking (순서대로 연결해줍니다)
        // restClient.get() -> uri() -> retrieve() -> body()
        given(restClient.get()).willReturn(requestHeadersUriSpec);
        given(requestHeadersUriSpec.uri(any(URI.class))).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);

        // body()가 호출될 때 클래스 타입에 따라 다른 리턴값을 주도록 설정
        given(responseSpec.body(eq(WelfareApiDto.CentralResponse.class))).willReturn(listResponse); // 목록 요청 시
        given(responseSpec.body(eq(WelfareApiDto.DetailItem.class))).willReturn(detailItem);      // 상세 요청 시

        given(welfareRepository.findByServId("WLF_TEST_001")).willReturn(Optional.empty());

        // when
        welfareService.syncCentralWelfareData();

        // then
        verify(welfareRepository, atLeastOnce()).save(any(Welfare.class));
    }

    @Test
    @DisplayName("3. 수집 테스트(필터링): '영유아' 데이터는 저장되지 않아야 한다.")
    void syncWelfareFilterTest() {
        // given
        WelfareApiDto.CentralItem listItem = new WelfareApiDto.CentralItem();
        listItem.setServId("WLF_TEST_002");
        listItem.setServNm("아동 수당");
        listItem.setLifeArray("영유아");

        WelfareApiDto.CentralResponse listResponse = new WelfareApiDto.CentralResponse();
        listResponse.setServList(List.of(listItem));

        // [변경 3] RestClient 체이닝 Mocking
        given(restClient.get()).willReturn(requestHeadersUriSpec);
        given(requestHeadersUriSpec.uri(any(URI.class))).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.body(eq(WelfareApiDto.CentralResponse.class))).willReturn(listResponse);

        // when
        welfareService.syncCentralWelfareData();

        // then
        verify(welfareRepository, never()).save(any(Welfare.class));
    }
}