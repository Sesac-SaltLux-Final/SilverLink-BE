package com.aicc.silverlink.infra.external.sms;

import com.solapi.sdk.SolapiClient;
import com.solapi.sdk.message.exception.SolapiMessageNotReceivedException;
import com.solapi.sdk.message.model.Message;
import com.solapi.sdk.message.service.DefaultMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Slf4j
@Component
public class SolapiSmsSender {

    @Value("${solapi.api-key}")
    private String apiKey;

    @Value("${solapi.api-secret}")
    private String apiSecret;

    @Value("${solapi.sender-number}")
    private String fromNumber;

    private DefaultMessageService messageService;

    @PostConstruct
    public void init() {
        // Solapi MessageService 초기화 (한 번만)
        this.messageService = SolapiClient.INSTANCE.createInstance(apiKey, apiSecret);
        log.info("[Solapi] MessageService initialized. fromNumber={}", maskPhone(fromNumber));
    }

    /**
     * 단문 SMS 발송
     * @param to 수신번호 (예: 01012345678)
     * @param text 메시지 본문
     */
    public void sendSms(String to, String text) {
        Message message = new Message();
        message.setFrom(fromNumber);  // 계정에 등록한 발신번호
        message.setTo(to);
        message.setText(text);

        try {
            messageService.send(message);
            log.info("[Solapi] SMS sent. to={}, len={}", maskPhone(to), (text == null ? 0 : text.length()));
        } catch (SolapiMessageNotReceivedException e) {
            // 실패한 메시지 목록 확인 가능
            log.error("[Solapi] SMS failed. to={}, failedList={}, msg={}",
                    maskPhone(to), e.getFailedMessageList(), e.getMessage(), e);
            throw new IllegalStateException("SMS 발송 실패(Solapi): " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("[Solapi] SMS error. to={}, msg={}", maskPhone(to), e.getMessage(), e);
            throw new IllegalStateException("SMS 발송 중 오류: " + e.getMessage(), e);
        }
    }

    // 로그에 전화번호 그대로 찍히는 거 방지용(실무에서 추천)
    private String maskPhone(String phone) {
        if (phone == null) return null;
        int len = phone.length();
        if (len <= 4) return "****";
        return phone.substring(0, Math.min(3, len)) + "****" + phone.substring(Math.max(len - 4, 0));
    }
}
