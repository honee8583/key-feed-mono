package com.keyfeed.keyfeedmonolithic.infra.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.keyfeed.keyfeedmonolithic.domain.crawl.scheduler.CrawlScheduler;
import com.keyfeed.keyfeedmonolithic.domain.payment.scheduler.BillingScheduler;
import com.keyfeed.keyfeedmonolithic.domain.payment.scheduler.SubscriptionExpiryScheduler;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

class SchedulerLockSpecTest {

    private static final List<Class<?>> SCHEDULER_CLASSES = List.of(
            BillingScheduler.class,
            SubscriptionExpiryScheduler.class,
            CrawlScheduler.class
    );

    private List<Method> scheduledMethods() {
        return SCHEDULER_CLASSES.stream()
                .flatMap(clazz -> Arrays.stream(clazz.getDeclaredMethods()))
                .filter(method -> method.isAnnotationPresent(Scheduled.class))
                .toList();
    }

    @Test
    @DisplayName("모든 @Scheduled 메서드에는 @SchedulerLock이 존재한다")
    void 모든_스케줄드_메서드에_스케줄러락_존재() {
        // given
        List<Method> methods = scheduledMethods();

        // then
        assertThat(methods).isNotEmpty();
        assertThat(methods)
                .allSatisfy(method -> assertThat(method.isAnnotationPresent(SchedulerLock.class))
                        .as("%s#%s 메서드에 @SchedulerLock이 없습니다",
                                method.getDeclaringClass().getSimpleName(), method.getName())
                        .isTrue());
    }

    @Test
    @DisplayName("모든 @SchedulerLock의 name은 비어있지 않고 서로 중복되지 않는다")
    void 스케줄러락_이름은_비어있지_않고_중복되지_않음() {
        // given
        List<String> names = scheduledMethods().stream()
                .map(method -> method.getAnnotation(SchedulerLock.class))
                .map(SchedulerLock::name)
                .toList();

        // then
        assertThat(names).hasSize(5);
        assertThat(names).allSatisfy(name -> assertThat(name).isNotBlank());
        assertThat(names).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("모든 @SchedulerLock의 lockAtMostFor는 파싱 가능하고 0보다 크다")
    void 스케줄러락_lockAtMostFor는_파싱_가능하고_양수() {
        // given
        List<SchedulerLock> locks = scheduledMethods().stream()
                .map(method -> method.getAnnotation(SchedulerLock.class))
                .toList();

        // then
        assertThat(locks).allSatisfy(lock -> {
            Duration lockAtMostFor = Duration.parse(lock.lockAtMostFor());
            assertThat(lockAtMostFor)
                    .as("%s의 lockAtMostFor는 0보다 커야 합니다", lock.name())
                    .isPositive();
        });
    }

    @Test
    @DisplayName("모든 @SchedulerLock의 lockAtLeastFor는 파싱 가능하고 lockAtMostFor 이하이다")
    void 스케줄러락_lockAtLeastFor는_파싱_가능하고_lockAtMostFor_이하() {
        // given
        List<SchedulerLock> locks = scheduledMethods().stream()
                .map(method -> method.getAnnotation(SchedulerLock.class))
                .toList();

        // then
        assertThat(locks).allSatisfy(lock -> {
            Duration lockAtLeastFor = Duration.parse(lock.lockAtLeastFor());
            Duration lockAtMostFor = Duration.parse(lock.lockAtMostFor());
            assertThat(lockAtLeastFor)
                    .as("%s의 lockAtLeastFor는 lockAtMostFor 이하여야 합니다", lock.name())
                    .isLessThanOrEqualTo(lockAtMostFor);
        });
    }

    @Test
    @DisplayName("SchedulerLockConfig에는 @EnableSchedulerLock이 존재하고 defaultLockAtMostFor가 유효하다")
    void 설정_클래스에_EnableSchedulerLock_존재하고_기본값_유효() {
        // given
        EnableSchedulerLock annotation = SchedulerLockConfig.class.getAnnotation(EnableSchedulerLock.class);

        // then
        assertThat(annotation).isNotNull();
        Duration defaultLockAtMostFor = Duration.parse(annotation.defaultLockAtMostFor());
        assertThat(defaultLockAtMostFor).isPositive();
    }
}
