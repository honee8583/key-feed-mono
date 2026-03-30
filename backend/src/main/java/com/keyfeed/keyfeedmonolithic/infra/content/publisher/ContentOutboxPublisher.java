package com.keyfeed.keyfeedmonolithic.infra.content.publisher;

import com.keyfeed.keyfeedmonolithic.infra.outbox.publisher.RedisQueuePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContentOutboxPublisher {

    private static final String QUEUE_KEY = "queue:content.created";

    private final RedisQueuePublisher redisQueuePublisher;

    public void publish(String payload) {
        redisQueuePublisher.publish(QUEUE_KEY, payload);
    }
}
