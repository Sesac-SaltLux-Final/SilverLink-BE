package com.aicc.silverlink.domain.notice.dto;

// Notice 클래스 안에 있는 것을 꺼내 쓴다고 명시
import com.aicc.silverlink.domain.notice.entity.Notice.NoticeStatus;
import com.aicc.silverlink.domain.notice.entity.Notice.TargetMode;
import com.aicc.silverlink.domain.user.entity.Role;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class NoticeRequest {

    private String title;
    private String content;

    // 타겟 설정 (ALL 또는 ROLE_SET)
    private TargetMode targetMode;
    // ROLE_SET일 경우 선택된 권한들 (ADMIN, COUNSELOR, GUARDIAN, ELDERLY)
    private List<Role> targetRoles;

    // Req 65: 중요 공지 여부
    private boolean isPriority;

    // Req 67: 팝업 설정
    private boolean isPopup;
    private LocalDateTime popupStartAt;
    private LocalDateTime popupEndAt;

    // Req 66: 첨부파일 (파일 업로드는 별도 로직으로 처리 후 메타데이터만 받거나, MultipartFile을 직접 받을 수 있음.
    // 여기서는 기획상 파일 정보를 리스트로 받는다고 가정)
    private List<NoticeAttachmentRequest> attachments;

    // 상태 (DRAFT, PUBLISHED 등 - 수정 시 사용)
    private NoticeStatus status;

    @Data
    public static class NoticeAttachmentRequest {
        private String fileName;
        private String originalFileName;
        private String filePath;
        private Long fileSize;
    }
}