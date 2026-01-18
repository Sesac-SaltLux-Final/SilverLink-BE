package com.aicc.silverlink.domain.welfare.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

public class WelfareApiDto {

    // [기존] 목록 조회용 (wantedList)
    @Getter @Setter @ToString
    @JacksonXmlRootElement(localName = "wantedList")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResponseWrapper<T> {
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "servList")
        private List<T> servList;
    }

    // [추가] 상세 조회용 (wantedDtl) - 목록과 루트 태그 이름이 다릅니다!
    @Getter @Setter @ToString
    @JacksonXmlRootElement(localName = "wantedDtl")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DetailResponseWrapper<T> {
        // 상세 조회는 리스트가 아니라 객체 하나가 바로 나옵니다.
        // 하지만 제네릭 구조상 편의를 위해 매핑을 유연하게 처리하거나,
        // 아래처럼 필드들을 직접 받아서 매핑합니다.

        // 여기서는 T타입(Item)의 필드들이 wantedDtl 바로 아래에 펼쳐져 있으므로
        // Jackson이 T타입으로 바로 변환하도록 유도하거나, 별도 Wrapper 없이 T를 씁니다.
        // 하지만 RestTemplate은 Root를 필요로 하므로,
        // 사실상 DetailItem 자체가 Root Element 역할을 하게 됩니다.
    }

    // ==========================================
    // 알맹이 데이터 Items
    // ==========================================

// WelfareApiDto.java 내부

    @Getter @Setter @NoArgsConstructor @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CentralItem {
        private String servId;
        private String servNm;
        private String jurMnofNm; // 중앙부처는 이 이름이 맞습니다.
        private String servDgst;
        private String tgtrDtlCn;
        private String slctCritCn;
        private String alwServCn;
        private String rprsCtadr;
        private String servDtlLink;
        private String lifeArray;

        // [추가] 카테고리 정보 (중앙부처용)
        private String intrsThemaArray;
    }

    @Getter @Setter @NoArgsConstructor @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LocalItem {
        private String servId;
        private String servNm;
        private String servDgst;
        private String slctCritCn;
        private String alwServCn;
        private String servDtlLink;
        private String ctpvNm;
        private String sggNm;
        private String lifeNmArray;

        // [추가] 부서명 (지자체용) -> 우리 DB의 jurMnofNm에 넣을 예정
        private String bizChrDeptNm;

        // [추가] 카테고리 정보 (지자체용)
        private String intrsThemaNmArray;

        // [추가] 문의처 (지자체는 inqNum 태그가 문의처 전화번호인 경우가 많음)
        private String inqNum;
    }

    // [추가] 상세 정보를 받을 통합 Item (중앙/지자체 공통 필드 최대한 활용)
    @Getter @Setter @NoArgsConstructor @ToString
    @JacksonXmlRootElement(localName = "wantedDtl") // 루트 태그 매핑
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DetailItem {
        private String servId;
        private String servNm;

        // 상세 내용 필드들
        private String tgtrDtlCn;    // 대상 상세 (중앙)
        private String sprtTrgtCn;   // 지원 대상 (지자체)

        private String slctCritCn;   // 선정 기준
        private String alwServCn;    // 급여 서비스 내용 (지원 내용)
        private String aplyMtdCn;    // 신청 방법

        // 문의처
        private String rprsCtadr;
    }

    // 제네릭 껍데기
    public static class CentralResponse extends ResponseWrapper<CentralItem> {}
    public static class LocalResponse extends ResponseWrapper<LocalItem> {}
}