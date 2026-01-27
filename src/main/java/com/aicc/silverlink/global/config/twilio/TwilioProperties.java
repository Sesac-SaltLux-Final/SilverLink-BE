package com.aicc.silverlink.global.config.twilio;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Twilio 설정 프로퍼티
 *
 * application.yml 예시:
 * twilio:
 *   account-sid: ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
 *   auth-token: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
 *   verify-service-sid: VAxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
 *   from-number: +821012345678
 *   sms-enabled: true
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "twilio")
public class TwilioProperties {

    /**
     * Twilio Account SID
     */
    private String accountSid;

    /**
     * Twilio Auth Token
     */
    private String authToken;

    /**
     * Twilio Verify Service SID (인증번호 발송용)
     */
    private String verifyServiceSid;

    /**
     * SMS 발송 번호 (E.164 형식: +821012345678)
     * 일반 SMS 발송 시 사용
     */
    private String fromNumber;

    /**
     * SMS 발송 활성화 여부
     * false로 설정 시 실제 SMS 발송 없이 로그만 기록
     */
    private boolean smsEnabled = true;
}
