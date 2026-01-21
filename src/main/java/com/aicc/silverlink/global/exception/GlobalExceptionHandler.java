package com.aicc.silverlink.global.exception;

import com.aicc.silverlink.global.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 로직 처리 중 발생하는 예외를 한곳에서 처리합니다.
     */
    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ApiResponse<?>> handleBusinessException(BusinessException e) {
        log.error("handleBusinessException: {}", e.getErrorCode().getMessage());
        ErrorCode errorCode = e.getErrorCode();

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode.getMessage()));
    }

    /**
     * 예상치 못한 일반적인 모든 예외를 처리합니다.
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        log.error("handleException", e);

        return ResponseEntity
                .status(500)
                .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }

    /**
     * 로그인 실패 / 잘못된 인자 (IllegalArgumentException)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        String message = e.getMessage();

        if ("LOGIN_FAIL".equals(message)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED) // 401
                    .body(Map.of("error", "LOGIN_FAIL", "message", "아이디 또는 비밀번호가 일치하지 않습니다"));
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST) // 400
                .body(Map.of("error", "BAD_REQUEST", "message", message));

    }

    /**
     * 상태 오류 (IllegalArgumentException)
     * 이미 로그인 중(BLOCK_NEW) , 계정 비활성화 등
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN) // 403
                .body(Map.of("error", "FORBIDDEN", "message", e.getMessage()));

    }

    /**
     * Spring Security 권한 거부 예외 처리 (Spring 6.3+)
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthorizationDenied(AuthorizationDeniedException e) {
        log.error("handleAuthorizationDenied: {}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("접근 권한이 없습니다."));
    }

    /**
     * Spring Security 권한 거부 예외 처리 (레거시 지원)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDenied(AccessDeniedException e) {
        log.error("handleAccessDenied: {}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("접근 권한이 없습니다."));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        // 첫 번째 에러 메시지를 가져옵니다 (예: "비밀번호는 필수입니다.")
        String errorMessage = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();

        log.warn("Validation Failed: {}", errorMessage);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST) // 400
                .body(ApiResponse.error(errorMessage));
    }

    /**
     * @Valid 유효성 검사 실패 시 (DTO의 @NotNull, @NotBlank 등 위반)
     * 500 에러가 아닌 400 Bad Request를 반환하도록 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ApiResponse<?>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("handleMethodArgumentNotValidException", e);

        // 에러가 발생한 필드 중 첫 번째 메시지만 가져와서 반환
        // (필요 시 모든 에러를 리스트로 반환하도록 수정 가능)
        String errorMessage = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : "잘못된 요청입니다.";

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(errorMessage));
    }

}