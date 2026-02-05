package com.aicc.silverlink.domain.emergency.event;

import com.aicc.silverlink.domain.notification.service.UnifiedSseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 긴급 알림 이벤트 리스너
 * 트랜잭션 커밋 후 SSE 알림을 발송하여, 프론트엔드가 DB 조회 시 데이터를 확인할 수 있도록 보장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmergencyAlertEventListener {

    private final UnifiedSseService unifiedSseService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEmergencyAlertCreated(EmergencyAlertCreatedEvent event) {
        try {
            unifiedSseService.sendEmergencyAlertToUsers(
                    event.getRecipientUserIds(),
                    event.getAlert());
            log.info("[EmergencyAlertEventListener] 트랜잭션 커밋 후 SSE 알림 발송 완료. alertId={}, 수신자 수={}",
                    event.getAlert().getId(), event.getRecipientUserIds().size());
        } catch (Exception e) {
            log.error("[EmergencyAlertEventListener] SSE 알림 발송 중 오류. alertId={}, error={}",
                    event.getAlert().getId(), e.getMessage(), e);
        }
    }
}
