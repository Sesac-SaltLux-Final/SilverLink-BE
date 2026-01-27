package com.aicc.silverlink.domain.notice.controller;

import com.aicc.silverlink.domain.admin.entity.Admin;
import com.aicc.silverlink.domain.notice.dto.NoticeRequest;
import com.aicc.silverlink.domain.notice.dto.NoticeResponse;
import com.aicc.silverlink.domain.notice.service.NoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "공지사항 관리 (관리자)", description = "공지사항 등록/삭제 API (관리자 전용)")
@RestController
@RequestMapping("/api/admin/notices")
@RequiredArgsConstructor
public class AdminNoticeController {

    private final NoticeService noticeService;

    // 공지사항 목록 조회
    @GetMapping
    public ResponseEntity<Page<NoticeResponse>> getAllNotices(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(noticeService.getAllNoticesForAdmin(pageable));
    }

    // 공지사항 등록
    @PostMapping
    public ResponseEntity<Long> createNotice(
            @RequestBody @Valid NoticeRequest request, // @Valid 추가
            @AuthenticationPrincipal Admin admin) { // Spring Security 사용 가정
        Long noticeId = noticeService.createNotice(request, admin);
        return ResponseEntity.ok(noticeId);
    }

    // 공지사항 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<NoticeResponse> getNotice(@PathVariable Long id) {
        return ResponseEntity.ok(noticeService.getNoticeDetail(id, null));
    }

    // Req 68: 공지사항 삭제 (Soft Delete)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotice(
            @PathVariable Long id,
            @AuthenticationPrincipal Admin admin) { // Admin 파라미터 추가
        noticeService.deleteNotice(id, admin);
        return ResponseEntity.ok().build();
    }

    // 수정 API는 create와 유사하므로 생략하거나 추가 가능
}
