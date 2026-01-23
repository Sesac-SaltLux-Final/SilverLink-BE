package com.aicc.silverlink.domain.elderly.controller;

import com.aicc.silverlink.domain.elderly.dto.response.ElderlySummaryResponse;
import com.aicc.silverlink.domain.elderly.dto.response.HealthInfoResponse;
import com.aicc.silverlink.domain.elderly.service.ElderlyService;
import com.aicc.silverlink.global.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "어르신", description = "어르신 정보 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/elderly")
public class ElderlyController {

    private final ElderlyService elderlyService;

    @PreAuthorize("hasAnyRole('ADMIN','COUNSELOR','GUARDIAN')")
    @GetMapping("/{elderlyUserId}/summary")
    public ElderlySummaryResponse summary(@PathVariable Long elderlyUserId) {
        return elderlyService.getSummary(elderlyUserId);
    }

    // 건강정보 조회(민감정보): 권한 체크 해야함
    @PreAuthorize("hasAnyRole('ADMIN','COUNSELOR','GUARDIAN')")
    @GetMapping("/{elderlyUserId}/health")
    public HealthInfoResponse health(@PathVariable Long elderlyUserId) {
        Long requesterId = SecurityUtils.currentUserId();
        return elderlyService.getHealthInfo(requesterId, elderlyUserId);
    }
}
