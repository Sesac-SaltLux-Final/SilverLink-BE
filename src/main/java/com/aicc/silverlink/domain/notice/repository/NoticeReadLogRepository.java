package com.aicc.silverlink.domain.notice.repository;

import com.aicc.silverlink.domain.notice.entity.NoticeReadLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticeReadLogRepository extends JpaRepository<NoticeReadLog, Long> {
    boolean existsByNoticeIdAndUserId(Long noticeId, String userId);

    // 이 메서드가 누락되었는지 확인하세요.
    List<NoticeReadLog> findByNoticeId(Long noticeId);
}
// 특정 사용자가 이미 이 공지를 확인했는지 체크 (중복 확인 방지)