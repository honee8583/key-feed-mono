package com.keyfeed.keyfeedmonolithic.infra.content.consumer;

import com.keyfeed.keyfeedmonolithic.infra.content.publisher.ContentOutboxPublisher;
import com.keyfeed.keyfeedmonolithic.infra.outbox.alert.OutboxExhaustedEvent;
import com.keyfeed.keyfeedmonolithic.infra.outbox.entity.Outbox;
import com.keyfeed.keyfeedmonolithic.infra.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentOutboxConsumer {

    private final OutboxRepository outboxRepository;
    private final ContentOutboxPublisher publisher;
    private final ApplicationEventPublisher alertPublisher;

    @Scheduled(fixedDelay = 1000, scheduler = "contentOutboxConsumerScheduler")
    public void process() {
        List<Outbox> pendingEvents = outboxRepository.findPendingEvents(LocalDateTime.now());
        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("[ContentOutboxConsumer] PENDING 이벤트 {}건 처리 시작", pendingEvents.size());

        long startTime = System.nanoTime();
        int success = 0;
        int fail = 0;
        for (Outbox outbox : pendingEvents) {
            boolean processSuccess = processEvent(outbox);
            if (processSuccess) {
                success++;
            } else {
                fail++;
            }
        }

        log.info("[ContentOutboxConsumer] 처리 완료 - 성공: {}건, 실패: {}건, 소요: {}ms",
                success,
                fail,
                (System.nanoTime() - startTime) / 1_000_000);
    }

    private boolean processEvent(Outbox outbox) {
        try {
            publisher.publish(outbox.getPayload());
            outbox.markPublished();
            log.info("[OutboxWorker] 발행 성공 - id: {}, contentId: {}", outbox.getId(), outbox.getAggregateId());

            return true;
        } catch (Exception e) {
            outbox.markFailed(e.getMessage());
            log.error("[OutboxWorker] 발행 실패 - id: {}, retryCount: {}, error: {}", outbox.getId(), outbox.getRetryCount(), e.getMessage());

            // 최대 재시도 초과 시 로그 저장
            if (outbox.isExhausted()) {
                alertPublisher.publishEvent(
                        new OutboxExhaustedEvent(outbox.getId(), outbox.getAggregateId())
                );
            }
            return false;
        } finally {
            outboxRepository.save(outbox);
        }
    }
}