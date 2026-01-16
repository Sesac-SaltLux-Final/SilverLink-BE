package com.aicc.silverlink.domain.welfare.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 공공데이터 수집 전용 DTO 그룹
 */
public class WelfareApiDto {

    // [1] 중앙부처 API 전용 (NationalWelfare)
    @Getter @Setter @NoArgsConstructor
    public static class CentralItem {
        private String servId;
        private String servNm;
        private String jurMnofNm;
        private String wlfareInfoOutlCn; // 상세조회 시 요약 내용
        private String tgtrDtlCn;        // 대상자 상세 (주의: tgtr)
        private String slctCritCn;       // 선정 기준
        private String alwServCn;        // 지원 내용
        private String rprsCtadr;        // 문의처
        private String servDtlLink;      // 상세 링크
    }

    // [2] 지자체 API 전용 (LocalGovernment)
    @Getter @Setter @NoArgsConstructor
    public static class LocalItem {
        private String servId;
        private String servNm;
        private String servDgst;         // 서비스 요약
        private String sprtTrgtCn;       // 지원 대상 (주의: sprtTrgt)
        private String slctCritCn;       // 선정 기준
        private String alwServCn;        // 급여 서비스 내용
        private String inqNum;           // 조회수(또는 문의처로 사용됨)
        private String servDtlLink;      // 상세 링크
        private String ctpvNm;           // 시도명 (서울, 경기 등)
        private String sggNm;            // 시군구명 (강남구, 수원시 등)
    }
}