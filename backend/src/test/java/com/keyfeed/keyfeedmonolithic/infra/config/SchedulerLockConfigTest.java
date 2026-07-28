package com.keyfeed.keyfeedmonolithic.infra.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class SchedulerLockConfigTest {

    private static DriverManagerDataSource dataSource;
    private static LockProvider lockProvider;

    @BeforeAll
    static void 데이터소스와_락프로바이더_초기화() throws SQLException {
        dataSource = new DriverManagerDataSource("jdbc:h2:mem:shedlocktest;DB_CLOSE_DELAY=-1;MODE=MYSQL", "sa", "");

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    create table if not exists shedlock (
                        name varchar(64) not null,
                        lock_until timestamp(3) not null,
                        locked_at timestamp(3) not null,
                        locked_by varchar(255) not null,
                        primary key (name)
                    )
                    """);
        }

        lockProvider = new SchedulerLockConfig().lockProvider(dataSource);
    }

    @BeforeEach
    void 락_테이블_초기화() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("delete from shedlock");
        }
    }

    private LockConfiguration lockConfiguration(String name, Duration lockAtMostFor, Duration lockAtLeastFor) {
        return new LockConfiguration(Instant.now(), name, lockAtMostFor, lockAtLeastFor);
    }

    @Test
    @DisplayName("락 획득에 성공한다")
    void 락_획득_성공() {
        // given
        LockConfiguration configuration =
                lockConfiguration("lock-acquire", Duration.ofSeconds(10), Duration.ZERO);

        // when
        Optional<SimpleLock> lock = lockProvider.lock(configuration);

        // then
        assertThat(lock).isPresent();
    }

    @Test
    @DisplayName("동일한 이름의 락은 중복 획득에 실패한다")
    void 동일_이름_락_중복_획득_실패() {
        // given
        LockConfiguration configuration =
                lockConfiguration("lock-duplicate", Duration.ofSeconds(10), Duration.ZERO);
        Optional<SimpleLock> firstLock = lockProvider.lock(configuration);
        assertThat(firstLock).isPresent();

        // when
        Optional<SimpleLock> secondLock = lockProvider.lock(
                lockConfiguration("lock-duplicate", Duration.ofSeconds(10), Duration.ZERO));

        // then
        assertThat(secondLock).isEmpty();
    }

    @Test
    @DisplayName("unlock 이후에는 동일한 이름의 락을 다시 획득할 수 있다")
    void unlock_후_재획득_성공() {
        // given
        Optional<SimpleLock> firstLock = lockProvider.lock(
                lockConfiguration("lock-release", Duration.ofSeconds(10), Duration.ZERO));
        assertThat(firstLock).isPresent();

        // when
        firstLock.get().unlock();
        Optional<SimpleLock> secondLock = lockProvider.lock(
                lockConfiguration("lock-release", Duration.ofSeconds(10), Duration.ZERO));

        // then
        assertThat(secondLock).isPresent();
    }

    @Test
    @DisplayName("lockAtLeastFor 동안에는 unlock 하더라도 락이 유지된다")
    void lockAtLeastFor_동안_락_유지() throws InterruptedException {
        // given
        Optional<SimpleLock> firstLock = lockProvider.lock(
                lockConfiguration("lock-at-least", Duration.ofSeconds(10), Duration.ofMillis(500)));
        assertThat(firstLock).isPresent();

        // when
        firstLock.get().unlock();
        Optional<SimpleLock> lockDuringAtLeastFor = lockProvider.lock(
                lockConfiguration("lock-at-least", Duration.ofSeconds(10), Duration.ZERO));

        Thread.sleep(700);
        Optional<SimpleLock> lockAfterAtLeastFor = lockProvider.lock(
                lockConfiguration("lock-at-least", Duration.ofSeconds(10), Duration.ZERO));

        // then
        assertThat(lockDuringAtLeastFor).isEmpty();
        assertThat(lockAfterAtLeastFor).isPresent();
    }

    @Test
    @DisplayName("다른 이름의 락은 독립적으로 획득할 수 있다")
    void 다른_이름_락_독립_획득() {
        // given
        Optional<SimpleLock> lockA = lockProvider.lock(
                lockConfiguration("lock-a", Duration.ofSeconds(10), Duration.ZERO));
        assertThat(lockA).isPresent();

        // when
        Optional<SimpleLock> lockB = lockProvider.lock(
                lockConfiguration("lock-b", Duration.ofSeconds(10), Duration.ZERO));

        // then
        assertThat(lockB).isPresent();
    }
}
