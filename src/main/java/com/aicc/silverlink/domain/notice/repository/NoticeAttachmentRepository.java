package com.aicc.silverlink.domain.notice.repository;

import com.aicc.silverlink.domain.notice.entity.NoticeAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticeAttachmentRepository extends JpaRepository<NoticeAttachment, Long> {

    // 특정 공지사항에 속한 첨부파일 목록을 가져옵니다.
    List<NoticeAttachment> findAllByNoticeId(Long noticeId);
}