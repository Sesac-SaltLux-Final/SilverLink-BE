package com.aicc.silverlink.domain.map.dto;

import com.aicc.silverlink.domain.map.entity.WelfareFacilityType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class WelfareFacilityRequest {
    private String name;
    private String address;
    private Double latitude;
    private Double longitude;
    private WelfareFacilityType type;
    private String phone;
    private String operatingHours;
}
