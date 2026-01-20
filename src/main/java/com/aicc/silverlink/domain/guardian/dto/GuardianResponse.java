package com.aicc.silverlink.domain.guardian.dto;


import com.aicc.silverlink.domain.guardian.entity.Guardian;
import com.aicc.silverlink.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuardianResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;

    private String addressLine1;
    private String addressLine2;
    private String zipcode;

    public static GuardianResponse from(Guardian guardian) {
        User user = guardian.getUser();

        return GuardianResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())

                .addressLine1(guardian.getAddressLine1())
                .addressLine2(guardian.getAddressLine2())
                .zipcode(guardian.getZipcode())
                .build();
    }


}
