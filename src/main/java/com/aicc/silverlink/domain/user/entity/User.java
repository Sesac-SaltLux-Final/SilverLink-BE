package com.aicc.silverlink.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name="login_id", nullable=false, unique=true, length=50)
    private String loginId;

    @Column(name="password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name="role", nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable = false)
    private UserStatus status;

    @Column(name="name", nullable = false, length = 50)
    private String name;

    @Column(name="phone", nullable = false, length = 20)
    private String phone;

    @Column(name="email", length = 100)
    private String email;

    @Column(name="phone_verified", nullable = false)
    private boolean phoneVerified;

    @Column(name="phone_verified_at")
    private LocalDateTime phoneVerifiedAt;

    @Column(name="created_by")
    private Long createdBy;

    @Column(name="last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name="created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name="updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name="deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = UserStatus.ACTIVE;
        }
        if (!phoneVerified == false) {
            this.phoneVerified = false;
        }
    }

    public void updateLastLogin(){
        this.lastLoginAt = LocalDateTime.now();
    }


    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }


    public static User createLocal(String loginId, String encodedPassword, String name, String phone, String email, Role role){
        validate(loginId, encodedPassword, name, phone, role);
        return User.builder()
                .loginId(loginId.trim())
                .passwordHash(encodedPassword)
                .name(name)
                .phone(normalizePhone(phone))
                .email(email)
                .role(role)
                .status(UserStatus.ACTIVE)
                .phoneVerified(false)
                .build();
    }

    public void changePassword(String newEncodedPassword) {
        if (newEncodedPassword == null || newEncodedPassword.isBlank()) {
            throw new IllegalArgumentException("PASSWORD_INVALID");
        }
        this.passwordHash = newEncodedPassword;
    }

    public void suspend() { this.status = UserStatus.LOCKED; }
    public void activate() { this.status = UserStatus.ACTIVE; }
    public void softDelete() { 
        this.status = UserStatus.DELETED; 
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    private static void validate(String loginId, String encodedPassword, String name, String phone, Role role) {
        if (loginId == null || loginId.isBlank()) throw new IllegalArgumentException("LOGIN_ID_INVALID");
        if (encodedPassword == null || encodedPassword.isBlank()) throw new IllegalArgumentException("PASSWORD_INVALID");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("NAME_INVALID");
        if (phone == null || phone.isBlank()) throw new IllegalArgumentException("PHONE_INVALID");
        if (role == null) throw new IllegalArgumentException("ROLE_INVALID");
    }

    private static String normalizePhone(String phone) {
        return phone.replaceAll("[^0-9]", "");
    }
}
