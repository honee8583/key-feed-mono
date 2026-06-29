package com.keyfeed.keyfeedmonolithic.domain.auth.repository;

import com.keyfeed.keyfeedmonolithic.domain.auth.entity.EmailPurpose;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class EmailVerificationCacheRepository {

    private final StringRedisTemplate redisTemplate;

    private static final String CODE_KEY = "email:verify:%s:%s";
    private static final String LOCK_KEY = "email:verify:lock:%s:%s";
    private static final String DONE_KEY = "email:verify:done:%s:%s";
    private static final String COOLDOWN_KEY = "email:verify:cooldown:%s:%s";

    private static final String FIELD_CODE = "code";
    private static final String FIELD_ATTEMPT = "attemptCount";

    public void saveCode(EmailPurpose purpose, String email, String code, Duration ttl) {
        String key = codeKey(purpose, email);
        redisTemplate.opsForHash().putAll(key, Map.of(FIELD_CODE, code, FIELD_ATTEMPT, "0"));
        redisTemplate.expire(key, ttl);
    }

    public String getCode(EmailPurpose purpose, String email) {
        Object code = redisTemplate.opsForHash().get(codeKey(purpose, email), FIELD_CODE);
        return code == null ? null : code.toString();
    }

    public long increaseAttempt(EmailPurpose purpose, String email) {
        return redisTemplate.opsForHash().increment(codeKey(purpose, email), FIELD_ATTEMPT, 1L);
    }

    public long getCodeTtlSeconds(EmailPurpose purpose, String email) {
        Long ttl = redisTemplate.getExpire(codeKey(purpose, email), TimeUnit.SECONDS);
        return ttl == null || ttl < 0 ? 0L : ttl;
    }

    public void deleteCode(EmailPurpose purpose, String email) {
        redisTemplate.delete(codeKey(purpose, email));
    }

    public boolean isLocked(EmailPurpose purpose, String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey(purpose, email)));
    }

    public void lock(EmailPurpose purpose, String email, Duration ttl) {
        redisTemplate.opsForValue().set(lockKey(purpose, email), "1", ttl);
    }

    public boolean isDone(EmailPurpose purpose, String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(doneKey(purpose, email)));
    }

    public void markDone(EmailPurpose purpose, String email, Duration ttl) {
        redisTemplate.opsForValue().set(doneKey(purpose, email), "1", ttl);
    }

    public void deleteDone(EmailPurpose purpose, String email) {
        redisTemplate.delete(doneKey(purpose, email));
    }

    public boolean tryCooldown(EmailPurpose purpose, String email, Duration ttl) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(cooldownKey(purpose, email), "1", ttl));
    }

    private String codeKey(EmailPurpose purpose, String email) {
        return String.format(CODE_KEY, purpose.name(), email);
    }

    private String lockKey(EmailPurpose purpose, String email) {
        return String.format(LOCK_KEY, purpose.name(), email);
    }

    private String doneKey(EmailPurpose purpose, String email) {
        return String.format(DONE_KEY, purpose.name(), email);
    }

    private String cooldownKey(EmailPurpose purpose, String email) {
        return String.format(COOLDOWN_KEY, purpose.name(), email);
    }
}
