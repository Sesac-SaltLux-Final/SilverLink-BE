package com.aicc.silverlink.domain.elderly.dto;

import lombok.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 통화 스케줄 관련 DTO
 */
public class CallScheduleDto {

    // ===== 요청 DTO =====

    /**
     * 통화 스케줄 설정/수정 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String preferredCallTime; // "09:00" 형식
        private List<String> preferredCallDays; // ["MON", "WED", "FRI"]
        private Boolean callScheduleEnabled;

        /**
         * 요일 리스트를 콤마 구분 문자열로 변환
         */
        public String getDaysAsString() {
            if (preferredCallDays == null || preferredCallDays.isEmpty()) {
                return null;
            }
            return String.join(",", preferredCallDays);
        }
    }

    // ===== 응답 DTO =====

    /**
     * 스케줄 조회 응답
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class Response {
        private Long elderlyId;
        private String elderlyName;
        private String preferredCallTime;
        private List<String> preferredCallDays;
        private Boolean callScheduleEnabled;

        public static Response from(com.aicc.silverlink.domain.elderly.entity.Elderly elderly) {
            return Response.builder()
                    .elderlyId(elderly.getId())
                    .elderlyName(elderly.getUser().getName())
                    .preferredCallTime(elderly.getPreferredCallTime())
                    .preferredCallDays(parseDays(elderly.getPreferredCallDays()))
                    .callScheduleEnabled(elderly.getCallScheduleEnabled())
                    .build();
        }

        private static List<String> parseDays(String days) {
            if (days == null || days.isBlank()) {
                return List.of();
            }
            return Arrays.asList(days.split(","));
        }
    }

    /**
     * Python CallBot 호출용 요청 DTO
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class StartCallRequest {
        private Long elderlyId;
        private String elderlyName;
        private String phone;
        private List<String> chronicDiseases;

        /**
         * 만성질환 문자열(TEXT)을 리스트로 변환
         */
        public static List<String> parseChronicDiseases(String chronicDiseases) {
            if (chronicDiseases == null || chronicDiseases.isBlank()) {
                return List.of();
            }
            return Arrays.stream(chronicDiseases.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
    }
}
