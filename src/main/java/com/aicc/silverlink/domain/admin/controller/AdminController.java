package com.aicc.silverlink.domain.admin.controller;

import com.aicc.silverlink.domain.admin.dto.request.AdminCreateRequest;
import com.aicc.silverlink.domain.admin.dto.request.AdminUpdateRequest;
import com.aicc.silverlink.domain.admin.dto.response.AdminResponse;
import com.aicc.silverlink.domain.admin.entity.Admin.AdminLevel;
import com.aicc.silverlink.domain.admin.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 관리자 API Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/admins")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * 관리자 생성
     * POST /api/admins
     */
    @PostMapping
    public ResponseEntity<AdminResponse> createAdmin(
            @Valid @RequestBody AdminCreateRequest request) {
        log.info("POST /api/admins - 관리자 생성 요청");
        AdminResponse response = adminService.createAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 관리자 정보 조회
     * GET /api/admins/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<AdminResponse> getAdmin(@PathVariable Long userId) {
        log.info("GET /api/admins/{} - 관리자 조회", userId);
        AdminResponse response = adminService.getAdmin(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 관리자 목록 조회 (다양한 필터 지원)
     * GET /api/admins
     * - Query Parameters:
     *   - admDongCode: 행정동 코드로 필터링
     *   - level: 관리자 레벨로 필터링 (NATIONAL, PROVINCIAL, CITY, DISTRICT)
     */
    @GetMapping
    public ResponseEntity<List<AdminResponse>> getAdmins(
            @RequestParam(required = false) Long admDongCode,
            @RequestParam(required = false) AdminLevel level) {

        if (admDongCode != null) {
            log.info("GET /api/admins?admDongCode={} - 행정동별 관리자 조회", admDongCode);
            List<AdminResponse> responses = adminService.getAdminsByAdmDongCode(admDongCode);
            return ResponseEntity.ok(responses);
        }

        if (level != null) {
            log.info("GET /api/admins?level={} - 레벨별 관리자 조회", level);
            List<AdminResponse> responses = adminService.getAdminsByLevel(level);
            return ResponseEntity.ok(responses);
        }

        log.info("GET /api/admins - 전체 관리자 조회");
        List<AdminResponse> responses = adminService.getAllAdmins();
        return ResponseEntity.ok(responses);
    }

    /**
     * 특정 행정구역의 상위 관리자들 조회
     * GET /api/admins/supervisors?admDongCode={code}
     * 예: 역삼동의 상위 관리자 → 강남구, 서울시, 전국 관리자
     */
    @GetMapping("/supervisors")
    public ResponseEntity<List<AdminResponse>> getSupervisors(
            @RequestParam Long admDongCode) {
        log.info("GET /api/admins/supervisors?admDongCode={} - 상위 관리자 조회", admDongCode);
        List<AdminResponse> responses = adminService.getSupervisors(admDongCode);
        return ResponseEntity.ok(responses);
    }

    /**
     * 특정 관리자의 하위 관리자들 조회
     * GET /api/admins/{userId}/subordinates
     * 예: 강남구 관리자의 하위 → 역삼동, 삼성동 등
     */
    @GetMapping("/{userId}/subordinates")
    public ResponseEntity<List<AdminResponse>> getSubordinates(
            @PathVariable Long userId) {
        log.info("GET /api/admins/{}/subordinates - 하위 관리자 조회", userId);
        List<AdminResponse> responses = adminService.getSubordinates(userId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 관리자 권한 확인
     * GET /api/admins/{userId}/jurisdiction?targetCode={code}
     * 특정 관리자가 특정 행정구역에 대한 권한이 있는지 확인
     */
    @GetMapping("/{userId}/jurisdiction")
    public ResponseEntity<Boolean> checkJurisdiction(
            @PathVariable Long userId,
            @RequestParam Long targetCode) {
        log.info("GET /api/admins/{}/jurisdiction?targetCode={} - 권한 확인",
                userId, targetCode);
        boolean hasJurisdiction = adminService.hasJurisdiction(userId, targetCode);
        return ResponseEntity.ok(hasJurisdiction);
    }

    /**
     * 관리자 정보 수정
     * PUT /api/admins/{userId}
     */
    @PutMapping("/{userId}")
    public ResponseEntity<AdminResponse> updateAdmin(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUpdateRequest request) {
        log.info("PUT /api/admins/{} - 관리자 정보 수정", userId);
        AdminResponse response = adminService.updateAdmin(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 관리자 삭제
     * DELETE /api/admins/{userId}
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteAdmin(@PathVariable Long userId) {
        log.info("DELETE /api/admins/{} - 관리자 삭제", userId);
        adminService.deleteAdmin(userId);
        return ResponseEntity.noContent().build();
    }
}