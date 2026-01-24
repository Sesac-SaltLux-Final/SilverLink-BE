package com.aicc.silverlink.domain.admin.controller;

import com.aicc.silverlink.domain.admin.dto.AdminMemberDtos;
import com.aicc.silverlink.domain.admin.service.OfflineRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "관리자-회원관리", description = "오프라인 대면 회원가입 및 관리")
@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminMemberController {

    private final OfflineRegistrationService registrationService;

    @Operation(summary = "어르신 오프라인 등록", description = "센터 방문 어르신을 대면 등록합니다.")
    @PostMapping("/elderly")
    public ResponseEntity<Long> registerElderly(@Valid @RequestBody AdminMemberDtos.RegisterElderlyRequest req) {
        Long userId = registrationService.registerElderly(req);
        return ResponseEntity.ok(userId);
    }

    @Operation(summary = "보호자 오프라인 등록", description = "센터 방문 보호자를 대면 등록하고 어르신과 연결합니다.")
    @PostMapping("/guardian")
    public ResponseEntity<Long> registerGuardian(@Valid @RequestBody AdminMemberDtos.RegisterGuardianRequest req) {
        Long userId = registrationService.registerGuardian(req);
        return ResponseEntity.ok(userId);
    }
}
