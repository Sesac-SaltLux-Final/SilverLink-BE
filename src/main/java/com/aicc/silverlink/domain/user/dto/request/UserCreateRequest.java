package com.aicc.silverlink.domain.user.dto.request;

import com.aicc.silverlink.domain.user.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserCreateRequest(
    @NotBlank String loginId,
    @NotBlank String password,
    @NotBlank String phone,
    @NotNull Role role
){}
