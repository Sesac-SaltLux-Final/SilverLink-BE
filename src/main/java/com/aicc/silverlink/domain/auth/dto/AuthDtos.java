package com.aicc.silverlink.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public class AuthDtos {

    public record LoginRequest(
            @Schema(description = "사용자 로그인 ID", example = "silverlink111")
            @NotBlank(message = "로그인 ID는 필수입니다.")
            String loginId,

            @Schema(description = "사용자 비밀번호", example = "pass1234!")
            @NotBlank(message = "비밀번호는 필수입니다.")
            String password) {
    }

    public record TokenResponse(
            @Schema(description = "Access Token 값")
            String accessToken,

            @Schema(description = "토큰 만료 시간(초)")
            long expiresInSeconds,

            @Schema(description = "사용자 권한 (ADMIN, GUARDIAN 등)")
            String role
    ) {
    }


    public record RefreshResponse(
            @Schema(description = "새로 발급된 Access Token")
            String accessToken,

            @Schema(description = "토큰 만료 시간(초)")
            long expiresInSeconds
    ) {
    }

}