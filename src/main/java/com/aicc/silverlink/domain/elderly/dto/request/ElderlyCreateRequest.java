package com.aicc.silverlink.domain.elderly.dto.request;

import com.aicc.silverlink.domain.elderly.entity.Elderly;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ElderlyCreateRequest(
        @NotNull Long userId,
        @NotBlank @Size(max = 20) String admDongCode,
        @NotNull LocalDate birthDate,
        @NotNull Elderly.Gender gender,
        @Size(max = 200) String addressLine1,
        @Size(max = 200) String addressLine2,
        @Size(max = 10) String zipcode
    ){}
