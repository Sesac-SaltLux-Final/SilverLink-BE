package com.aicc.silverlink.domain.notice.entity;

import com.aicc.silverlink.domain.user.entity.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode // 복합키에는 필수입니다.
public class NoticeTargetRoleId implements Serializable {

    @Column(name = "notice_id") // DB 컬럼명은 언더바 사용
    private Long noticeId;      // 자바 변수명은 noticeId (에러 발생 지점)

    @Enumerated(EnumType.STRING)
    @Column(name = "targer_role") // 오타 주의: DB 설계서에 targer로 되어 있다면 그대로 사용
    private Role targetRole;
}