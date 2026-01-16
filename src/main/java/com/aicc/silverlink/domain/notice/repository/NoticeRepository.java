package com.aicc.silverlink.domain.notice.repository;

import com.aicc.silverlink.domain.notice.entity.Notice;
import com.aicc.silverlink.domain.notice.entity.Notice.NoticeStatus;
import com.aicc.silverlink.domain.user.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    // 관리자용: 삭제되지 않은 모든 공지 조회 (상태 필터링 가능)
    Page<Notice> findAllByStatusNot(NoticeStatus status, Pageable pageable);

    // Req 64, 65: 사용자용 공지 목록 조회
    // 조건 1: 상태가 PUBLISHED
    // 조건 2: 삭제되지 않음
    // 조건 3: TargetMode가 ALL 이거나, TargetRoles에 사용자의 Role이 포함됨
    // 정렬: 중요공지(Priority) 우선, 그 다음 최신순 (Pageable에서 Sort 처리 권장하지만 복잡한 조건이라 JPQL 사용)
    @Query("SELECT DISTINCT n FROM Notice n " +
            "LEFT JOIN NoticeTargetRole ntr ON n.id = ntr.notice.id " +
            "WHERE n.status = 'PUBLISHED' " +
            "AND n.deletedAt IS NULL " +
            "AND (n.targetMode = 'ALL' OR ntr.targetRole = :role) " +
            "ORDER BY n.isPriority DESC, n.createdAt DESC")
    Page<Notice> findAllForUser(@Param("role") Role role, Pageable pageable);

    // Req 67: 유효한 팝업 공지 조회
    @Query("SELECT DISTINCT n FROM Notice n " +
            "LEFT JOIN NoticeTargetRole ntr ON n.id = ntr.notice.id " +
            "WHERE n.status = 'PUBLISHED' " +
            "AND n.isPopup = true " +
            "AND :now BETWEEN n.popupStartAt AND n.popupEndAt " +
            "AND (n.targetMode = 'ALL' OR ntr.targetRole = :role)")
    List<Notice> findActivePopups(@Param("role") Role role, @Param("now") LocalDateTime now);
}