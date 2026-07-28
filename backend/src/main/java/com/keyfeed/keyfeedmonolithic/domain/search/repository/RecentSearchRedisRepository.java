package com.keyfeed.keyfeedmonolithic.domain.search.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class RecentSearchRedisRepository {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "search:recent:";
    private static final int MAX_COUNT = 10;
    private static final Duration TTL = Duration.ofDays(30);

    public void add(Long userId, String keyword) {
        String key = key(userId);
        redisTemplate.opsForZSet().add(key, keyword, System.currentTimeMillis());
        redisTemplate.opsForZSet().removeRange(key, 0, -(MAX_COUNT + 1));
        redisTemplate.expire(key, TTL);
    }

    public List<String> findAll(Long userId) {
        Set<String> keywords = redisTemplate.opsForZSet().reverseRange(key(userId), 0, MAX_COUNT - 1);
        if (keywords == null) {
            return List.of();
        }
        return new ArrayList<>(keywords);
    }

    public void remove(Long userId, String keyword) {
        redisTemplate.opsForZSet().remove(key(userId), keyword);
    }

    public void removeAll(Long userId) {
        redisTemplate.delete(key(userId));
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }
}
