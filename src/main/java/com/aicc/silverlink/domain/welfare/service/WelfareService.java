package com.aicc.silverlink.domain.welfare.service;

import com.aicc.silverlink.domain.welfare.repository.WelfareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
@RequiredArgsConstructor
@Slf4j
public class WelfareService {

    private final WelfareRepository welfareRepository;
    private final ModelMapper modelMapper;

    @Value("${api.welfare.service-key}")
    private String serviceKey;

    @Value(("${api.welfare.central-url}"))
    private String centralUrl;

    @Value("${api.welfare.local-url}")
    private String localUrl;

    public void syncCentralWelfareData() {

        // 1. 중앙부처용 URL 조립 (지자체와 거의 동일하지만 URL이 다름)
        URI uri = UriComponentsBuilder.fromUriString(centralUrl) // [중요] localUrl 아님!
                .queryParam("serviceKey", serviceKey)
                .queryParam("numOfRows", "100")
                .queryParam("pageNo", "1")
                .queryParam("type", "xml")
                .queryParam("lifeArray", "006")  // [핵심] 여기도 독거노인(006)만 타겟팅!
                .build(true)
                .toUri();

        log.info("중앙부처 노인 복지 API 호출 URL: {}", uri);


    }
}
