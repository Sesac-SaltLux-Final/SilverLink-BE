package com.aicc.silverlink.domain.user.entity;

import com.aicc.silverlink.global.common.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
//@Table(name = "user")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name="login_id", nullable=false, unique=true, length=50)
    private String loginId;

    @Column(nullable=false)
    private String password;

    @Column(nullable=false, length=20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=20)
    private UserRole role;

    @Column(nullable=false)
    private boolean enabled;
}
