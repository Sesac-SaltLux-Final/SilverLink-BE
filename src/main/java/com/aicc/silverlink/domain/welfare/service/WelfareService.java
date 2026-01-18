package com.aicc.silverlink.domain.welfare.service;

import com.aicc.silverlink.domain.welfare.dto.*;
import com.aicc.silverlink.domain.welfare.entity.Source;
import com.aicc.silverlink.domain.welfare.entity.Welfare;
import com.aicc.silverlink.domain.welfare.repository.WelfareRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WelfareService {

    private final WelfareRepository welfareRepository;
    private final ModelMapper modelMapper;

    // [변경] RestTemplate -> RestClient
    private final RestClient restClient;

    @Value("${api.welfare.service-key}")
    private String serviceKey;

    @Value("${api.welfare.central-url}")
    private String centralUrl;

    @Value("${api.welfare.local-url}")
    private String localUrl;

    @Value("${api.welfare.central-detail-url}")
    private String centralDetailUrl;

    @Value("${api.welfare.local-detail-url}")
    private String localDetailUrl;

    // =================================================================================
    // [1] 데이터 조회 로직
    // =================================================================================

    @Transactional(readOnly = true)
    public Page<WelfareListResponse> searchWelfare(WelfareSearchRequest request, Pageable pageable) {
        Page<Welfare> welfarePage = welfareRepository.searchByKeyword(request.getKeyword(), pageable);
        return welfarePage.map(welfare -> modelMapper.map(welfare, WelfareListResponse.class));
    }

    @Transactional(readOnly = true)
    public WelfareDetailResponse getWelfareDetail(Long welfareId) {
        Welfare welfare = welfareRepository.findById(welfareId)
                .orElseThrow(() -> new IllegalArgumentException("해당 복지 서비스를 찾을 수 없습니다. ID=" + welfareId));
        return modelMapper.map(welfare, WelfareDetailResponse.class);
    }

    // =================================================================================
    // [2] 데이터 수집 및 동기화 로직
    // =================================================================================

    @Scheduled(cron = "0 0 4 * * *")
    public void syncAllWelfareScheduled() {
        log.info("=== [스케줄러 시작] 노인 복지 데이터 전수 동기화 ===");
        long startTime = System.currentTimeMillis();

        syncCentralWelfareData();
        syncLocalWelfareData();

        long endTime = System.currentTimeMillis();
        log.info("=== [스케줄러 종료] 소요 시간: {}ms ===", (endTime - startTime));
    }

    @Transactional
    public void syncCentralWelfareData() {
        syncDataWithPagination(centralUrl, WelfareApiDto.CentralResponse.class, Source.CENTRAL);
    }

    @Transactional
    public void syncLocalWelfareData() {
        syncDataWithPagination(localUrl, WelfareApiDto.LocalResponse.class, Source.LOCAL);
    }

    private <T, R extends WelfareApiDto.ResponseWrapper<T>> void syncDataWithPagination(String baseUrl, Class<R> responseType, Source source) {
        int pageNo = 1;
        int numOfRows = 100;
        boolean hasMoreData = true;

        while (hasMoreData) {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl)
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("numOfRows", numOfRows)
                    .queryParam("pageNo", pageNo)
                    .queryParam("lifeArray", "006");

            if (source == Source.CENTRAL) {
                builder.queryParam("callTp", "L");
                builder.queryParam("srchKeyCode", "001");
            }

            URI uri = builder.encode().build().toUri();

            try {
                log.info("[{}] {}페이지 요청 중...", source, pageNo);

                // [변경] RestClient 사용 (Fluent API)
                R response = restClient.get()
                        .uri(uri)
                        .retrieve()
                        .body(responseType);

                if (response != null && response.getServList() != null) {
                    List<T> items = response.getServList();

                    if (items == null || items.isEmpty()) {
                        hasMoreData = false;
                        log.info("[{}] 더 이상 데이터가 없습니다. (페이지: {})", source, pageNo);
                    } else {
                        for (T item : items) {
                            saveOrUpdate(item, source);
                        }

                        if (items.size() < numOfRows) {
                            hasMoreData = false;
                            log.info("[{}] 마지막 페이지입니다. (총 {}건 수집)", source, items.size());
                        } else {
                            pageNo++;
                        }
                    }
                } else {
                    hasMoreData = false;
                    log.warn("[{}] 응답 데이터(Body)가 비어있어 종료합니다.", source);
                }
            } catch (Exception e) {
                log.error("[{}] {}페이지 동기화 중 에러 발생: {}", source, pageNo, e.getMessage());
                hasMoreData = false;
            }
        }
    }

    private <T> void saveOrUpdate(T item, Source source) {
        // 1. 노년 필터링
        if (item instanceof WelfareApiDto.CentralItem c) {
            if (c.getLifeArray() == null || !c.getLifeArray().contains("노년")) return;
        } else if (item instanceof WelfareApiDto.LocalItem l) {
            if (l.getLifeNmArray() == null || !l.getLifeNmArray().contains("노년")) return;
        }

        String servId = getServIdFromItem(item);

        // 2. 엔티티 조회 또는 생성
        Welfare welfare = welfareRepository.findByServId(servId).orElseGet(Welfare::new);

        // 3. 매핑 (Config에 설정된 규칙대로 자동 변환!)
        modelMapper.map(item, welfare);

        // 4. 상세 정보 병합
        fetchAndMergeDetail(servId, welfare, source);

        // 5. 공통 필드 및 저장
        welfare.setSource(source);


        welfareRepository.save(welfare);
    }

    private void fetchAndMergeDetail(String servId, Welfare welfare, Source source) {
        try {
            String detailUrl = (source == Source.CENTRAL) ? centralDetailUrl : localDetailUrl;

            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(detailUrl)
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("servId", servId);

            if (source == Source.CENTRAL) {
                builder.queryParam("callTp", "D");
            }

            URI uri = builder.encode().build().toUri();

            // [변경] RestClient 사용
            WelfareApiDto.DetailItem detail = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(WelfareApiDto.DetailItem.class);

            if (detail != null) {
                if (detail.getAlwServCn() != null) welfare.setAlwServCn(detail.getAlwServCn());
                if (detail.getSlctCritCn() != null) welfare.setSlctCritCn(detail.getSlctCritCn());

                if (detail.getTgtrDtlCn() != null) welfare.setTargetDtlCn(detail.getTgtrDtlCn()); // 중앙
                if (detail.getSprtTrgtCn() != null) welfare.setTargetDtlCn(detail.getSprtTrgtCn()); // 지자체

                // 문의처가 상세에만 있는 경우 보완
                if (welfare.getRprsCtadr() == null && detail.getRprsCtadr() != null) {
                    welfare.setRprsCtadr(detail.getRprsCtadr());
                }
            }
        } catch (Exception e) {
            log.warn("상세 정보 수집 실패 (ID: {}): {}", servId, e.getMessage());
        }
    }

    private String getServIdFromItem(Object item) {
        if (item instanceof WelfareApiDto.CentralItem c) return c.getServId();
        if (item instanceof WelfareApiDto.LocalItem l) return l.getServId();
        return null;
    }
}