package com.aicc.silverlink.global.common.response;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        boolean hasNext
) {
    // Page 객체(Spring Data)를 받아 변환하는 생성자를 만들어두면 편리합니다.
}