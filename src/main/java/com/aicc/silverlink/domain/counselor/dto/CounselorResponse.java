package com.aicc.silverlink.domain.counselor.dto;

import com.aicc.silverlink.domain.counselor.entity.Counselor;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Builder
@Getter
@NoArgsConstructor
public class CounselorResponse {

    private Long id;

    private String name;
    private String loginId;
    private String email;
    private String phone;

    private String employeeNo;
    private String department;

    private UserStatus status;
    private String officePhone;

    private String admDongCode;

    public static CounselorResponse from(Counselor counselor){
        User user = counselor.getUser();

        return CounselorResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .loginId(user.getLoginId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus())
                .department(counselor.getDepartment())
                .employeeNo(counselor.getEmployeeNo())
                .admDongCode(counselor.getAdmDongCode())
                .build();
    }
}
