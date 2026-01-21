package com.aicc.silverlink.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserRequests {

    public record UpdateMyProfileRequest(
            @NotBlank @Size(max = 50) String name,
            @Email @Size(max = 100) String email
    ) {}

    public record ChangeStatusRequest(
            @NotBlank String status
    ){}
}
