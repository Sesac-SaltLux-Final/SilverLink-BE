package com.aicc.silverlink.domain.notice.controller;

import com.aicc.silverlink.domain.notice.dto.NoticeResponse;
import com.aicc.silverlink.domain.notice.service.NoticeService;
import com.aicc.silverlink.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "공지사항", description = "공지사항 조회 API")
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    // Req 64, 65: 내 권한에 맞는 공지사항 목록 조회 (중요공지 상단)
    @GetMapping
    public ResponseEntity<Page<NoticeResponse>> getMyNotices(
            @AuthenticationPrincipal User user, // 현재 로그인한 사용자
            @RequestParam(required = false) String keyword, // 검색 키워드 추가
            Pageable pageable) {
        return ResponseEntity.ok(noticeService.getNoticesForUser(user, keyword, pageable));
    }

    // Req 67: 메인 화면 팝업 공지 조회
    @GetMapping("/popups")
    public ResponseEntity<List<NoticeResponse>> getPopups(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(noticeService.getActivePopupsForUser(user));
    }

    // 공지사항 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<NoticeResponse> getNoticeDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(noticeService.getNoticeDetail(id, user));
    }

    // Req 69: 공지사항 필독 확인 ("확인했습니다" 버튼 클릭 시)
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> readNotice(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        noticeService.readNotice(id, user);
        return ResponseEntity.ok().build();
    }
}
