package com.aicc.silverlink.domain.admin.entity;

import com.aicc.silverlink.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "admin")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Admin {
    @Id
    @Column(name = "user_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "adm_dong_code", nullable = false, length = 20)
    private String admDongCode;

    public Admin(User user, String admDongCode) {
        this.user = user;
        this.admDongCode = admDongCode;
    }
}
