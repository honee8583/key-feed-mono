package com.keyfeed.keyfeedmonolithic.domain.payment.scheduler;

import com.keyfeed.keyfeedmonolithic.domain.notification.dto.NotificationEventDto;
import com.keyfeed.keyfeedmonolithic.domain.notification.service.NotificationService;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.*;
import com.keyfeed.keyfeedmonolithic.domain.payment.exception.InvalidPaymentMethodException;
import com.keyfeed.keyfeedmonolithic.domain.payment.exception.PaymentFailedException;
import com.keyfeed.keyfeedmonolithic.domain.payment.repository.PaymentHistoryRepository;
import com.keyfeed.keyfeedmonolithic.domain.payment.repository.SubscriptionRepository;
import com.keyfeed.keyfeedmonolithic.domain.payment.service.BillingExecutor;
import com.keyfeed.keyfeedmonolithic.domain.payment.writer.SubscriptionWriter;
import com.keyfeed.keyfeedmonolithic.global.client.toss.TossPaymentsClient;
import com.keyfeed.keyfeedmonolithic.global.client.toss.dto.response.TossPaymentQueryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingScheduler {

    private static final int MAX_RETRY_COUNT = 3;
    private static final int READY_STALE_MINUTES = 10;
    private static final String PAYMENT_FAILED_ALERT_TITLE = "구독 결제 실패 안내";
    private static final String PAYMENT_FAILED_ALERT_MESSAGE = "결제가 3회 실패하여 구독이 일시 정지되었습니다. 결제 수단을 변경해주세요.";

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final TossPaymentsClient tossPaymentsClient;
    private final NotificationService notificationService;
    private final BillingExecutor billingExecutor;
    private final SubscriptionWriter subscriptionWriter;

    /**
     * 자동 결제 스케줄러 — 매일 오전 10시 실행
     */
    // TODO 서버 인스턴스가 늘어날 경우 중복 결제 위험성
    @Scheduled(cron = "0 0 10 * * *")
    @Transactional // TODO 단일 트랜잭션으로 분리
    public void executeScheduledPayments() {
        log.info("[BillingScheduler] 자동 결제 시작");

        // 1. 결제 대상 구독 목록 조회 (status=ACTIVE, nextBillingAt <= 현재)
        List<Subscription> targets = subscriptionRepository
                .findByStatusAndNextBillingAtLessThanEqual(SubscriptionStatus.ACTIVE, LocalDateTime.now());

        log.info("[BillingScheduler] 결제 대상 구독 수: {}", targets.size());

        // 2. 각 구독에 대해 순차 처리
        for (Subscription subscription : targets) {
            processSubscription(subscription);
        }

        log.info("[BillingScheduler] 자동 결제 완료");
    }

    /**
     * 서버 재시작 시 복구 로직
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        recoverReadyPayments();
        recoverPendingSubscriptions();
    }

    /**
     * READY 상태 복구 로직
     * 10분 이상 READY 상태로 남아있는 건을 Toss API로 상태 확인 후 동기화
     */
    @Transactional
    public void recoverReadyPayments() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(READY_STALE_MINUTES);

        // 1. 10분 이상 READY 상태인 결제 이력 조회
        List<PaymentHistory> staleReadyList = paymentHistoryRepository
                .findByStatusAndCreatedAtBefore(PaymentHistoryStatus.READY, threshold);

        if (staleReadyList.isEmpty()) {
            return;
        }

        log.info("[BillingScheduler] READY 상태 복구 대상: {}건", staleReadyList.size());

        for (PaymentHistory history : staleReadyList) {
            recoverHistory(history);
        }
    }

    /**
     * PENDING 상태 구독 복구 로직
     * 결제 플로우 도중 서버가 중단된 경우 PENDING 상태로 방치된 구독을 복구한다.
     * 매 시간 실행되며 서버 시작 시에도 onApplicationReady()를 통해 호출된다.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void recoverPendingSubscriptions() {
        // 1. 결제 플로우 진행 중인 구독과 구분하기 위해 임계 시간(30분) 이전에 생성된 PENDING 구독만 조회
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(SubscriptionConstants.PENDING_STALE_MINUTES);
        List<Subscription> pendingList = subscriptionRepository
                .findByStatusAndCreatedAtBefore(SubscriptionStatus.PENDING, threshold);

        if (pendingList.isEmpty()) {
            return;
        }

        log.info("[BillingScheduler] PENDING 구독 복구 대상: {}건", pendingList.size());

        for (Subscription subscription : pendingList) {
            try {
                // 2. 해당 구독에 연결된 DONE 결제 내역 조회
                //    - DONE 존재: 결제는 완료됐으나 updateActive가 실패한 케이스 → ACTIVE로 복구
                //    - DONE 없음: 결제 전 또는 결제 실패 후 방치된 케이스 → CANCELED로 정리
                paymentHistoryRepository
                        .findTopBySubscriptionIdAndStatusOrderByCreatedAtDesc(
                                subscription.getId(), PaymentHistoryStatus.DONE)
                        .ifPresentOrElse(
                                doneHistory -> {
                                    subscriptionWriter.updateActive(subscription);
                                    log.info("[BillingScheduler] PENDING→ACTIVE 복구 - subscriptionId: {}", subscription.getId());
                                },
                                () -> {
                                    subscriptionWriter.updateCanceled(subscription);
                                    log.warn("[BillingScheduler] 방치된 PENDING→CANCELED 정리 - subscriptionId: {}", subscription.getId());
                                }
                        );
            } catch (Exception e) {
                // 3. 개별 구독 복구 실패 시 다음 구독 처리를 위해 예외를 삼키고 로그만 기록
                log.error("[BillingScheduler] PENDING 복구 실패 - subscriptionId: {}", subscription.getId(), e);
            }
        }
    }

    private void processSubscription(Subscription subscription) {
        Long userId = subscription.getUser().getId();

        try {
            // 결제 실행 (READY 선저장 → chargeBilling → markDone/markFailed)
            billingExecutor.execute(
                    subscription.getUser(),
                    subscription.getPaymentMethod(),
                    subscription,
                    subscription.getOrderName(),
                    subscription.getPrice()
            );

            // 결제 성공: nextBillingAt +1달 갱신, retryCount 초기화
            subscription.updateNextBillingAt(subscription.getNextBillingAt().plusMonths(1));
            subscription.resetRetryCount();

            log.info("[BillingScheduler] 결제 성공 - subscriptionId: {}, userId: {}", subscription.getId(), userId);

        } catch (PaymentFailedException | InvalidPaymentMethodException e) {
            // 카드 거부/만료 등 결제 실패: retryCount +1
            subscription.increaseRetryCount();

            log.warn("[BillingScheduler] 결제 실패 - subscriptionId: {}, retryCount: {}, reason: {}",
                    subscription.getId(), subscription.getRetryCount(), e.getMessage());

            // retryCount >= 3이면 PAUSED 전환 + 알림 발송
            if (subscription.getRetryCount() >= MAX_RETRY_COUNT) {
                subscription.pause();
                sendPaymentFailedNotification(userId);
                log.warn("[BillingScheduler] 구독 PAUSED 전환 - subscriptionId: {}, userId: {}", subscription.getId(), userId);
            }
        } catch (Exception e) {
            // 인프라/네트워크 오류: retryCount 증가 없이 로그만 기록
            log.error("[BillingScheduler] 결제 중 예상 외 오류 - subscriptionId: {}, error: {}", subscription.getId(), e.getMessage());
        }
    }

    private void recoverHistory(PaymentHistory history) {
        try {
            TossPaymentQueryResponse queryResponse = tossPaymentsClient.getPaymentByOrderId(history.getOrderId());

            if ("DONE".equals(queryResponse.getStatus())) {
                // 실제로 결제 성공 → DONE으로 동기화
                history.markDone(queryResponse.getPaymentKey(), parseApprovedAt(queryResponse.getApprovedAt()));
                log.info("[BillingScheduler] READY 복구(DONE) - orderId: {}", history.getOrderId());
            } else {
                // 결제 미완료 → FAILED 처리
                history.markFailed("서버 재시작으로 인한 결제 미완료");
                log.info("[BillingScheduler] READY 복구(FAILED) - orderId: {}", history.getOrderId());
            }
        } catch (Exception e) {
            // Toss 조회 실패 시 안전하게 FAILED 처리
            history.markFailed("복구 중 Toss API 조회 실패: " + e.getMessage());
            log.error("[BillingScheduler] READY 복구 실패 - orderId: {}, error: {}", history.getOrderId(), e.getMessage());
        }
    }

    private void sendPaymentFailedNotification(Long userId) {
        try {
            notificationService.send(NotificationEventDto.builder()
                    .userId(userId)
                    .title(PAYMENT_FAILED_ALERT_TITLE)
                    .message(PAYMENT_FAILED_ALERT_MESSAGE)
                    .build());
        } catch (Exception e) {
            log.error("[BillingScheduler] 알림 발송 실패 - userId: {}, error: {}", userId, e.getMessage());
        }
    }

    private LocalDateTime parseApprovedAt(String approvedAt) {
        if (approvedAt == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(approvedAt).toLocalDateTime();
        } catch (Exception e) {
            log.warn("[BillingScheduler] approvedAt 파싱 실패: {}", approvedAt);
            return null;
        }
    }
}
