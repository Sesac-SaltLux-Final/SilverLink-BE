package com.aicc.silverlink.domain.auth.controller;

import com.aicc.silverlink.domain.auth.dto.AuthDtos;
import com.aicc.silverlink.domain.auth.service.AuthService;
import com.aicc.silverlink.domain.auth.service.WebAuthnService;
import com.aicc.silverlink.global.config.auth.AuthPolicyProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/passkey")
@RequiredArgsConstructor
public class PasskeyController {

    private final WebAuthnService webAuthnService;
    private final AuthService authService;
    private final AuthPolicyProperties props;

    public record StartRegReq(@NotNull Long userId) {}
    public record FinishRegReq(@NotNull Long userId, @NotBlank String requestId, @NotBlank String credentialJson) {}

    @PostMapping("/register/options")
    public WebAuthnService.StartRegResponse startReg(@RequestBody StartRegReq req) throws JsonProcessingException {
        return webAuthnService.startRegistration(req.userId());
    }

    @PostMapping("/register/verify")
    public void finishReg(@RequestBody FinishRegReq req) {
        webAuthnService.finishRegistration(req.userId(), req.requestId(), req.credentialJson(), req.userId());
    }

    public record StartLoginReq(String loginId) {}
    public record FinishLoginReq(@NotBlank String requestId, @NotBlank String credentialJson) {}

    @PostMapping("/login/options")
    public WebAuthnService.StartAuthResponse startLogin(@RequestBody StartLoginReq req) throws JsonProcessingException {
        return webAuthnService.startAssertion(req.loginId());
    }

    @PostMapping("/login/verify")
    public AuthDtos.TokenResponse finishLogin(
            @Valid @RequestBody FinishLoginReq req,
            HttpServletRequest http,
            HttpServletResponse res
    ) {
        Long userId = webAuthnService.finishAssertion(req.requestId(), req.credentialJson());
        AuthService.AuthResult result = authService.issueForUser(userId, http);

        setRefreshCookie(res, result.sid() + "." + result.refreshToken());
        return new AuthDtos.TokenResponse(result.accessToken(), result.ttl(), "USER");
    }

    private void setRefreshCookie(HttpServletResponse res, String value) {
        ResponseCookie cookie = ResponseCookie.from(props.getRefreshCookieName(), value)
                .httpOnly(true)
                .secure(true)
                .path(props.getRefreshCookiePath())
                .maxAge(props.getRefreshTtlSeconds())
                .sameSite(props.getRefreshCookieSameSite())
                .build();
        res.addHeader("Set-Cookie", cookie.toString());
    }


}
