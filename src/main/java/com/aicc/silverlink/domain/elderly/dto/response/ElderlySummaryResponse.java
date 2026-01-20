package com.aicc.silverlink.domain.elderly.dto.response;

import com.aicc.silverlink.domain.elderly.entity.Elderly;

import java.time.LocalDate;

public record ElderlySummaryResponse(
        Long userId,
        String name,
        String phone,
        String admDongCode,
        LocalDate birthDate,
        int age,
        Elderly.Gender gender,
        String addressLine1,
        String addressLine2,
        String zipcode
) {
    public static ElderlySummaryResponse from(Elderly e) {
        return new ElderlySummaryResponse(
                e.getId(),
                e.getUser().getName(),
                e.getUser().getPhone(),
                e.getAdmDongCode(),
                e.getBirthDate(),
                e.age(),
                e.getGender(),
                e.getAddressLine1(),
                e.getAddressLine2(),
                e.getZipcode()
        );
    }
}
