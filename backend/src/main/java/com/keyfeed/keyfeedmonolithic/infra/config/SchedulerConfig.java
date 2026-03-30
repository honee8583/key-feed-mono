package com.keyfeed.keyfeedmonolithic.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedulerConfig {

    // ContentOutboxConsumer : outbox 테이블을 polling 후 Redis Queue content 삽입
    @Bean("contentOutboxConsumerScheduler")
    public TaskScheduler contentOutboxConsumerScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("content-outbox-consumer-");
        scheduler.initialize();
        return scheduler;
    }

    // NotificationConsumer : Redis Queue 조회 후 알림 저장
    @Bean("notificationConsumerScheduler")
    public TaskScheduler notificationConsumerScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(3);
        scheduler.setThreadNamePrefix("notification-consumer-");
        scheduler.initialize();
        return scheduler;
    }
}
