package com.keyfeed.keyfeedmonolithic.domain.payment.scheduler;

import com.keyfeed.keyfeedmonolithic.domain.payment.entity.Subscription;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.SubscriptionStatus;
import com.keyfeed.keyfeedmonolithic.domain.payment.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionExpiryScheduler {

    private final SubscriptionRepository subscriptionRepository;

    /**
     * 구독 만료 스케줄러 — 매일 자정 실행
     * status = CANCELED AND expired_at <= 현재 인 구독을 INACTIVE로 전환
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void expireSubscriptions() {
        log.info("[구독 만료 스케줄러] 시작 - 대상 조회");

        List<Subscription> expiredList = subscriptionRepository
                .findByStatusAndExpiredAtLessThanEqual(SubscriptionStatus.CANCELED, LocalDateTime.now());

        for (Subscription sub : expiredList) {
            sub.expire();
            log.info("[구독 만료] subscriptionId={}, userId={}", sub.getId(), sub.getUser().getId());
        }

        log.info("[구독 만료 스케줄러] {}건 INACTIVE 전환 완료", expiredList.size());
    }
}
