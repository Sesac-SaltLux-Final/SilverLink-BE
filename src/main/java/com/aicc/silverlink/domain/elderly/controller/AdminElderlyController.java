package com.aicc.silverlink.domain.elderly.controller;

import com.aicc.silverlink.domain.elderly.dto.request.ElderlyCreateRequest;
import com.aicc.silverlink.domain.elderly.dto.request.HealthInfoUpdateRequest;
import com.aicc.silverlink.domain.elderly.dto.response.ElderlySummaryResponse;
import com.aicc.silverlink.domain.elderly.dto.response.HealthInfoResponse;
import com.aicc.silverlink.domain.elderly.service.ElderlyService;
import com.aicc.silverlink.global.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "어르신 관리 (관리자)", description = "어르신 등록/수정 API (관리자 전용)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/elderly")
@PreAuthorize("hasRole('ADMIN')")
public class AdminElderlyController {

    private final ElderlyService elderlyService;

    @PostMapping
    public ElderlySummaryResponse create(@Valid @RequestBody ElderlyCreateRequest req) {
        return elderlyService.createElderly(req);
    }

    @PatchMapping("/{elderlyUserId}/health")
    public HealthInfoResponse upsertHealth(
            @PathVariable Long elderlyUserId,
            @Valid @RequestBody HealthInfoUpdateRequest req) {
        Long adminId = SecurityUtils.currentUserId();
        return elderlyService.upsertHealthInfo(adminId, elderlyUserId, req);
    }
}
