package com.aicc.silverlink.domain.elderly.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ElderlyUpdateRequest(
        @NotBlank String name,
        @NotBlank String phone,
        String addressLine1,
        String addressLine2,
        String zipcode
) {}