package com.aicc.silverlink.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public class AuthDtos {

        public record LoginRequest(
                        @Schema(description = "사용자 로그인 ID", example = "silverlink111") @NotBlank(message = "로그인 ID는 필수입니다.") String loginId,

                        @Schema(description = "사용자 비밀번호", example = "pass1234!") @NotBlank(message = "비밀번호는 필수입니다.") String password) {
        }

        public record TokenResponse(
                        @Schema(description = "Access Token 값") String accessToken,

                        @Schema(description = "토큰 만료 시간(초)") long expiresInSeconds,

                        @Schema(description = "사용자 권한 (ADMIN, GUARDIAN 등)") String role) {
        }

        public record RefreshResponse(
                        @Schema(description = "새로 발급된 Access Token") String accessToken,

                        @Schema(description = "토큰 만료 시간(초)") long expiresInSeconds) {
        }

        public record PhoneLoginRequest(
                        @Schema(description = "휴대폰 번호 (하이픈 없이)", example = "01012345678") @NotBlank(message = "휴대폰 번호는 필수입니다.") String phone,

                        @Schema(description = "휴대폰 인증 성공 후 발급된 proofToken") @NotBlank(message = "인증 토큰은 필수입니다.") String proofToken) {
        }

        // 비밀번호 재설정 요청
        public record PasswordResetRequest(
                        @Schema(description = "로그인 ID", example = "silverlink111") @NotBlank String loginId,
                        @Schema(description = "휴대폰 인증 proofToken") @NotBlank String proofToken,
                        @Schema(description = "새 비밀번호", example = "newPass1234!") @NotBlank String newPassword) {
        }

        // 아이디 찾기 요청
        public record FindIdRequest(
                        @Schema(description = "이름") @NotBlank String name,
                        @Schema(description = "휴대폰 인증 proofToken") @NotBlank String proofToken) {
        }

        // 아이디 찾기 응답
        public record FindIdResponse(
                        @Schema(description = "마스킹된 로그인 ID", example = "user***") String maskedLoginId) {
        }
}
