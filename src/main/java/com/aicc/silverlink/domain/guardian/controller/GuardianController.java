package com.aicc.silverlink.domain.guardian.controller;

import com.aicc.silverlink.domain.guardian.dto.GuardianElderlyResponse;
import com.aicc.silverlink.domain.guardian.dto.GuardianRequest;
import com.aicc.silverlink.domain.guardian.dto.GuardianResponse;
import com.aicc.silverlink.domain.guardian.entity.RelationType;
import com.aicc.silverlink.domain.guardian.service.GuardianService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "보호자", description = "보호자 등록/조회/어르신 연결 API")
@RestController
@RequestMapping("/api/guardians")
@RequiredArgsConstructor
public class GuardianController {

    private final GuardianService guardianService;

    @PostMapping("/signup")
    public ResponseEntity<GuardianResponse> signup(@RequestBody GuardianRequest request) {
        GuardianResponse response = guardianService.register(request);
        return ResponseEntity.created((URI.create("/api/guardians/" + response.getId()))).body(response);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('GUARDIAN')")
    public ResponseEntity<GuardianResponse> getMyInfo(@AuthenticationPrincipal Long currentUserId) {
        return ResponseEntity.ok(guardianService.getGuardian(currentUserId));
    }

    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GuardianResponse> getGuardianByAdminId(@PathVariable Long id) {
        return ResponseEntity.ok(guardianService.getGuardian(id));
    }

    // 상담사 식별을 위해 @AuthenticationPrincipal 추가
    @GetMapping("/counselor/{id}")
    @PreAuthorize("hasRole('COUNSELOR')")
    public ResponseEntity<GuardianResponse> getGuardianByCounselorId(
            @PathVariable Long id,
            @AuthenticationPrincipal Long currentUserId) {
        return ResponseEntity.ok(guardianService.getGuardianForCounselor(id, currentUserId));
    }

    @GetMapping("/admin/{id}/elderly")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GuardianElderlyResponse> getElderlyByGuardianForAdmin(@PathVariable("id") Long guardianId) {
        return ResponseEntity.ok(guardianService.getElderlyByGuardian(guardianId));
    }

    // ✅ 상담사 식별을 위해 @AuthenticationPrincipal 추가
    @GetMapping("/counselor/{id}/elderly")
    @PreAuthorize("hasRole('COUNSELOR')")
    public ResponseEntity<GuardianElderlyResponse> getElderlyByGuardianForCounselor(
            @PathVariable("id") Long guardianId,
            @AuthenticationPrincipal Long currentUserId) {
        return ResponseEntity.ok(guardianService.getElderlyByGuardianForCounselor(guardianId, currentUserId));
    }

    @GetMapping("/me/elderly")
    @PreAuthorize("hasRole('GUARDIAN')")
    public ResponseEntity<GuardianElderlyResponse> getMyElderly(@AuthenticationPrincipal Long currentUserId) {
        return ResponseEntity.ok(guardianService.getElderlyByGuardian(currentUserId));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<GuardianResponse>> getAllGuardians() {
        return ResponseEntity.ok(guardianService.getAllGuardian());
    }

    @PostMapping("/{id}/connect")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> connectElderly(
            @PathVariable("id") Long guardianId,
            @RequestParam Long elderlyId,
            @RequestParam RelationType relationType) {
        guardianService.connectElderly(guardianId, elderlyId, relationType);
        return ResponseEntity.ok().build();
    }
}