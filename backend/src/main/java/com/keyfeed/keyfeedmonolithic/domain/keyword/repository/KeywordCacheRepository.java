package com.keyfeed.keyfeedmonolithic.domain.keyword.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class KeywordCacheRepository {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "keyword:users:";

    public void addUserToKeyword(String keyword, Long userId) {
        redisTemplate.opsForSet().add(KEY_PREFIX + keyword, String.valueOf(userId));
    }

    public void removeUserFromKeyword(String keyword, Long userId) {
        redisTemplate.opsForSet().remove(KEY_PREFIX + keyword, (Object) String.valueOf(userId));
    }
}
