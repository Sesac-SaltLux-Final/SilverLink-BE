package com.aicc.silverlink.domain.notice.entity;

import com.aicc.silverlink.domain.user.entity.Role;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notice_target_roles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(NoticeTargetRoleId.class)
@AllArgsConstructor // <-- 이게 있어야 빌더 에러가 안 납니다!
@Builder
public class NoticeTargetRole {

    @EmbeddedId
    private NoticeTargetRoleId id;

    @Id
    @MapsId("noticeId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id", nullable = false)
    private Notice notice;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "target_role", nullable = false)
    private Role targetRole;
}
