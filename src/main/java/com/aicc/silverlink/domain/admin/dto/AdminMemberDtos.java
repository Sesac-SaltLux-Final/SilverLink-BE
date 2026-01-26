package com.aicc.silverlink.domain.admin.dto;

import com.aicc.silverlink.domain.elderly.entity.Elderly;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class AdminMemberDtos {

        public record RegisterElderlyRequest(
                        @NotBlank String loginId,
                        @NotBlank String password,
                        @NotBlank String name,
                        @NotBlank String phone,
                        String email,

                        @NotNull Long admCode, // 행정동 코드
                        @NotNull LocalDate birthDate,
                        @NotNull Elderly.Gender gender,
                        @NotBlank String addressLine1,
                        String addressLine2,
                        String zipcode,

                        String memo // 오프라인 등록 메모
        ) {
        }

        public record RegisterGuardianRequest(
                        @NotBlank String loginId,
                        @NotBlank String password,
                        @NotBlank String name,
                        @NotBlank String phone,
                        String email,

                        @NotBlank String addressLine1,
                        String addressLine2,
                        String zipcode,

                        @NotNull Long elderlyUserId, // 연결할 어르신 ID
                        @NotBlank String relationType, // FAMILY, CAREGIVER etc.

                        String memo) {
        }
}
