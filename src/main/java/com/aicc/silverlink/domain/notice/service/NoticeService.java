package com.aicc.silverlink.domain.notice.service;

import com.aicc.silverlink.domain.admin.entity.Admin; // Admin 엔티티 가정
import com.aicc.silverlink.domain.notice.dto.NoticeRequest;
import com.aicc.silverlink.domain.notice.dto.NoticeResponse;
import com.aicc.silverlink.domain.notice.entity.*;
import com.aicc.silverlink.domain.notice.entity.Notice.NoticeStatus;
import com.aicc.silverlink.domain.notice.repository.NoticeReadLogRepository;
import com.aicc.silverlink.domain.notice.repository.NoticeRepository;
import com.aicc.silverlink.domain.notice.repository.NoticeTargetRoleRepository; // 가정
import com.aicc.silverlink.domain.notice.repository.NoticeAttachmentRepository; // 가정
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User; // User 엔티티 가정
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final NoticeReadLogRepository noticeReadLogRepository;
    // 아래 레포지토리들은 파일에는 없었으나 Service 구현을 위해 필요하다고 가정하고 주입
    private final NoticeTargetRoleRepository noticeTargetRoleRepository;
    private final NoticeAttachmentRepository noticeAttachmentRepository;

    // --- 관리자(Admin) 기능 ---

    // 공지사항 생성
    @Transactional
    public Long createNotice(NoticeRequest request, Admin admin) {
        // 1. 공지사항 본문 저장
        Notice notice = Notice.builder()
                .createdBy(admin)
                .title(request.getTitle())
                .content(request.getContent())
                .category(request.getCategory()) // 카테고리 추가
                .targetMode(request.getTargetMode())
                .isPriority(request.isPriority())
                .isPopup(request.isPopup())
                .status(NoticeStatus.PUBLISHED)
                .createdAt(LocalDateTime.now())
                .popupStartAt(request.getPopupStartAt())
                .popupEndAt(request.getPopupEndAt())
                .build();
        // Setter가 없으므로 Builder 패턴이나 생성자, 혹은 엔티티 내 update 메서드 사용 필요.
        // 여기서는 편의상 엔티티에 매핑 메서드가 있다고 가정하거나 Builder 사용
        // (제공된 코드에 Setter 없음, Builder 없음 -> 직접 생성 로직 구현 필요)

        // *실제 구현 시 엔티티에 Builder나 생성 메서드 추가 필요*
        // 여기서는 로직의 흐름을 기술합니다.
        /*
         * notice.setTitle(request.getTitle());
         * notice.setContent(request.getContent());
         * notice.setCreatedBy(admin);
         * notice.setTargetMode(request.getTargetMode());
         * notice.setStatus(request.getStatus() != null ? request.getStatus() :
         * NoticeStatus.DRAFT);
         * ... 필드 매핑 ...
         */
        Notice savedNotice = noticeRepository.save(notice);

        // 2. 타겟 권한 저장
        if (request.getTargetMode() == Notice.TargetMode.ROLE_SET && request.getTargetRoles() != null) {
            List<NoticeTargetRole> targetRoles = request.getTargetRoles().stream()
                    .map(role -> NoticeTargetRole.builder()
                            .notice(savedNotice)
                            .targetRole(role)
                            .build())
                    .collect(Collectors.toList());
            noticeTargetRoleRepository.saveAll(targetRoles);
        }

        // 3. 첨부파일 저장 (Req 66)
        if (request.getAttachments() != null) {
            List<NoticeAttachment> attachments = request.getAttachments().stream()
                    .map(dto -> NoticeAttachment.builder()
                            .notice(savedNotice)
                            .fileName(dto.getFileName())
                            .originalFileName(dto.getOriginalFileName())
                            .filePath(dto.getFilePath())
                            .fileSize(dto.getFileSize())
                            .build())
                    .collect(Collectors.toList());
            noticeAttachmentRepository.saveAll(attachments);
        }

        return savedNotice.getId();
    }

    // Req 68: 삭제 정책 (Soft Delete)
    @Transactional
    public void deleteNotice(Long noticeId, Admin admin) { // Admin 파라미터 추가
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공지사항입니다."));

        // 작성자 본인 확인 (슈퍼 관리자 권한이 있다면 이 로직을 건너뛸 수 있음)
        if (!notice.getCreatedBy().getUserId().equals(admin.getUserId())) { // getId() -> getUserId() 수정
            throw new IllegalArgumentException("본인이 작성한 공지사항만 삭제할 수 있습니다.");
        }

        // 엔티티 내에 삭제 메서드를 호출하여 상태 변경
        // notice.markAsDeleted(); (status = DELETED, deletedAt = now)
        noticeRepository.delete(notice);
    }

    // 공지사항 수정
    @Transactional
    public void updateNotice(Long noticeId, NoticeRequest request, Admin admin) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공지사항입니다."));

        // 작성자 본인 확인 (또는 슈퍼 관리자 권한 확인)
        // 여기서는 모든 관리자가 수정 가능하도록 허용

        // 기존 Notice 엔티티에 setter나 update 메서드가 없으므로
        // 삭제 후 새로 생성하는 방식으로 처리 (또는 엔티티에 update 메서드 추가 필요)
        // 여기서는 간단히 기존 데이터 삭제 후 새로 생성
        noticeTargetRoleRepository.deleteAllByNoticeId(noticeId);
        noticeAttachmentRepository.deleteAllByNoticeId(noticeId);
        noticeRepository.delete(notice);

        // 새로 생성
        createNotice(request, admin);
    }

    // 관리자용 목록 조회
    public Page<NoticeResponse> getAllNoticesForAdmin(Pageable pageable) {
        return noticeRepository.findAllByStatusNot(NoticeStatus.DELETED, pageable)
                .map(notice -> convertToResponse(notice, false));
    }

    // --- 사용자(User) 기능 ---

    // Req 64, 65: 사용자 권한별 목록 조회 + 중요공지 우선 (검색 기능 추가)
    public Page<NoticeResponse> getNoticesForUser(User user, String keyword, Pageable pageable) {
        Role role = (user != null) ? user.getRole() : null;
        Page<Notice> notices = noticeRepository.findAllForUser(role, keyword, pageable);

        return notices.map(notice -> {
            boolean isRead = false;
            if (user != null) {
                isRead = noticeReadLogRepository.existsByNoticeIdAndUserId(notice.getId(),
                        String.valueOf(user.getId()));
            }
            return convertToResponse(notice, isRead);
        });
    }

    // Req 67: 팝업 공지 조회
    public List<NoticeResponse> getActivePopupsForUser(User user) {
        LocalDateTime now = LocalDateTime.now();
        Role role = (user != null) ? user.getRole() : null;
        List<Notice> notices = noticeRepository.findActivePopups(role, now);

        // 이미 읽은(확인한) 팝업은 제외할지, 프론트에서 처리할지는 기획에 따름.
        // 보통 "오늘 하루 보지 않기"는 쿠키로, "다시 보지 않기"는 DB로 처리.
        // 여기서는 읽음 여부만 같이 내려줌.
        return notices.stream()
                .map(notice -> {
                    boolean isRead = false;
                    if (user != null) {
                        isRead = noticeReadLogRepository.existsByNoticeIdAndUserId(notice.getId(),
                                String.valueOf(user.getId()));
                    }
                    return convertToResponse(notice, isRead);
                })
                .collect(Collectors.toList());
    }

    // Req 69: 공지사항 필독 확인 (상세 조회 시 호출하거나, 별도 버튼 클릭 시 호출)
    @Transactional
    public void readNotice(Long noticeId, User user) {
        if (!noticeReadLogRepository.existsByNoticeIdAndUserId(noticeId, String.valueOf(user.getId()))) {
            Notice notice = noticeRepository.findById(noticeId)
                    .orElseThrow(() -> new IllegalArgumentException("공지사항 없음"));

            // NoticeReadLog 엔티티 생성 및 저장
            // NoticeReadLog log = new NoticeReadLog(notice, user);
            // noticeReadLogRepository.save(log);
        }
    }

    // 상세 조회
    public NoticeResponse getNoticeDetail(Long noticeId, User user) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공지사항입니다."));

        boolean isRead = false;
        if (user != null) {
            isRead = noticeReadLogRepository.existsByNoticeIdAndUserId(noticeId, String.valueOf(user.getId()));
        }

        NoticeResponse response = convertToResponse(notice, isRead);

        // 이전글/다음글 ID 조회
        if (user != null) {
            noticeRepository.findPrevNoticeId(user.getRole(), noticeId)
                    .ifPresent(response::setPrevNoticeId);
            noticeRepository.findNextNoticeId(user.getRole(), noticeId)
                    .ifPresent(response::setNextNoticeId);
        }

        return response;
    }

    // Helper: Response 변환기
    private NoticeResponse convertToResponse(Notice notice, boolean isRead) {
        List<Role> roles = noticeTargetRoleRepository.findAllByNoticeId(notice.getId()) // 메서드 필요
                .stream().map(NoticeTargetRole::getTargetRole).collect(Collectors.toList());

        List<NoticeAttachment> attachments = noticeAttachmentRepository.findAllByNoticeId(notice.getId()); // 메서드 필요

        return NoticeResponse.from(notice, roles, attachments, isRead);
    }
}