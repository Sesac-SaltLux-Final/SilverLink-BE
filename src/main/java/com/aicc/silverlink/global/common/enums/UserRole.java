package com.aicc.silverlink.global.common.enums;

public enum UserRole {
    ADMIN, COUNSELOR, GUARDIAN, ELDERLY;

    public String asAuthority() {
        return "ROLE_" + this.name();
    }
}
