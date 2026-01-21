package com.aicc.silverlink.domain.system.dto.response;

import com.aicc.silverlink.domain.system.entity.AdministrativeDivision;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AddressResponse {

    private Long admCode;
    private String sidoName;
    private String sigunguName;
    private String dongName;
    private String fullAddress;
    private String level;

    public static AddressResponse from(AdministrativeDivision division) {
        return AddressResponse.builder()
                .admCode(division.getAdmCode())
                .sidoName(division.getSidoName())
                .sigunguName(division.getSigunguName())
                .dongName(division.getDongName())
                .fullAddress(division.getFullAddress())
                .level(division.getLevel().name())
                .build();
    }
}