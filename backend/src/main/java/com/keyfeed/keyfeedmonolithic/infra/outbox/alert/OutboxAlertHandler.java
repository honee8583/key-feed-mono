package com.keyfeed.keyfeedmonolithic.infra.outbox.alert;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OutboxAlertHandler {

    @EventListener
    public void onExhausted(OutboxExhaustedEvent event) {
        // 실무에서는 Slack, PagerDuty, 이메일 등으로 알람
        log.error("[ALERT] Outbox 최대 재시도 초과 - outboxId: {}, contentId: {}", event.getOutboxId(), event.getContentId());
    }
}
