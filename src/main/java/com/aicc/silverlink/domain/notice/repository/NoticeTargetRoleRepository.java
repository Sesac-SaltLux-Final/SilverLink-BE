package com.aicc.silverlink.domain.notice.repository;

import com.aicc.silverlink.domain.notice.entity.NoticeTargetRole;
import com.aicc.silverlink.domain.notice.entity.NoticeTargetRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticeTargetRoleRepository extends JpaRepository<NoticeTargetRole, NoticeTargetRoleId> {

    // 특정 공지사항에 설정된 모든 타겟 권한 목록을 가져옵니다.
    List<NoticeTargetRole> findAllByNoticeId(Long noticeId);

    // 수정 시 기존 권한 설정을 삭제하기 위해 필요할 수 있습니다.
    void deleteAllByNoticeId(Long noticeId);
}