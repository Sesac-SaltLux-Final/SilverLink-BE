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

    // ==========================================
    // [1] 공통 껍데기 (제네릭 <T> 사용)
    // ==========================================

    @Getter @Setter @ToString
    @JacksonXmlRootElement(localName = "response")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResponseWrapper<T> {
        @JacksonXmlProperty(localName = "body")
        private Body<T> body;
    }

    @Getter @Setter @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body<T> {
        @JacksonXmlProperty(localName = "items")
        private Items<T> items;
    }

    @Getter @Setter @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Items<T> {
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "item")
        private List<T> itemList;
    }

    // ==========================================
    // [2] 실제 사용할 구체적인 클래스 (Service에서 이걸 호출)
    // ==========================================

    // 중앙부처용: ResponseWrapper에 <CentralItem>을 끼워넣음
    public static class CentralResponse extends ResponseWrapper<CentralItem> {}

    // 지자체용: ResponseWrapper에 <LocalItem>을 끼워넣음
    public static class LocalResponse extends ResponseWrapper<LocalItem> {}


    // ==========================================
    // [3] 알맹이 데이터 (Item)
    // ==========================================

    @Getter @Setter @NoArgsConstructor @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CentralItem {
        private String servId;
        private String servNm;
        private String jurMnofNm;
        private String wlfareInfoOutlCn;
        private String tgtrDtlCn;
        private String slctCritCn;
        private String alwServCn;
        private String rprsCtadr;
        private String servDtlLink;
    }

    @Getter @Setter @NoArgsConstructor @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LocalItem {
        private String servId;
        private String servNm;
        private String servDgst;
        private String sprtTrgtCn;
        private String slctCritCn;
        private String alwServCn;
        private String inqNum;
        private String servDtlLink;
        private String ctpvNm;
        private String sggNm;
    }
}