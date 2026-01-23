package com.aicc.silverlink.domain.elderly.controller;

import com.aicc.silverlink.domain.elderly.dto.request.ElderlyCreateRequest;
import com.aicc.silverlink.domain.elderly.dto.request.HealthInfoUpdateRequest;
import com.aicc.silverlink.domain.elderly.dto.response.ElderlyAdminDetailResponse;
import com.aicc.silverlink.domain.elderly.dto.response.ElderlySummaryResponse;
import com.aicc.silverlink.domain.elderly.dto.response.HealthInfoResponse;
import com.aicc.silverlink.domain.elderly.service.ElderlyService;
import com.aicc.silverlink.global.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/elderly")
@PreAuthorize("hasRole('ADMIN')")
public class AdminElderlyController {

    private final ElderlyService elderlyService;

    // 관리자: 어르신 전체 목록 조회
    @GetMapping
    public List<ElderlySummaryResponse> listAll() {
        return elderlyService.getAllElderlyForAdmin();
    }

    // 관리자: 어르신 상세 통합 정보 조회 (보호자/상담사 포함)
    @GetMapping("/{elderlyUserId}/detail")
    public ElderlyAdminDetailResponse detail(@PathVariable Long elderlyUserId) {
        return elderlyService.getElderlyDetailForAdmin(elderlyUserId);
    }

    @PostMapping
    public ElderlySummaryResponse create(@Valid @RequestBody ElderlyCreateRequest req) {
        return elderlyService.createElderly(req);
    }

    @PatchMapping("/{elderlyUserId}/health")
    public HealthInfoResponse upsertHealth(
            @PathVariable Long elderlyUserId,
            @Valid @RequestBody HealthInfoUpdateRequest req
    ) {
        Long adminId = SecurityUtils.currentUserId();
        return elderlyService.upsertHealthInfo(adminId, elderlyUserId, req);
    }
}