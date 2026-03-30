package com.keyfeed.keyfeedmonolithic.infra.outbox.publisher;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisQueuePublisher {

    private final RedisTemplate<String, String> redisTemplate;

    public void publish(String queueKey, String payload) {
        redisTemplate.opsForList().leftPush(queueKey, payload);
    }
}
