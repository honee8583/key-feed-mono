package com.keyfeed.keyfeedmonolithic.domain.auth.repository;

import com.keyfeed.keyfeedmonolithic.domain.auth.entity.EmailPurpose;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 이메일 인증 코드/잠금/완료/쿨다운 상태를 Redis에 저장하는 저장소.
 *
 * <pre>
 * email:verify:{purpose}:{email}           Hash{code, attemptCount}  (인증 코드)
 * email:verify:lock:{purpose}:{email}      "1"                       (시도 초과 잠금)
 * email:verify:done:{purpose}:{email}      "1"                       (인증 완료, 멱등 처리)
 * email:verify:cooldown:{purpose}:{email}  "1"                       (재요청 쿨다운)
 * </pre>
 *
 * 만료는 모두 TTL로 자동 처리되며, 별도 정리(cleanup)가 필요 없다.
 */
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

    /**
     * 인증 코드를 저장한다(덮어쓰기 = 코드 교체 + 시도횟수 0 초기화 + TTL 갱신).
     */
    public void saveCode(EmailPurpose purpose, String email, String code, Duration ttl) {
        String key = codeKey(purpose, email);
        redisTemplate.opsForHash().putAll(key, Map.of(FIELD_CODE, code, FIELD_ATTEMPT, "0"));
        redisTemplate.expire(key, ttl);
    }

    /**
     * 저장된 인증 코드를 조회한다. TTL 만료/미발급이면 null.
     */
    public String getCode(EmailPurpose purpose, String email) {
        Object code = redisTemplate.opsForHash().get(codeKey(purpose, email), FIELD_CODE);
        return code == null ? null : code.toString();
    }

    /**
     * 시도 횟수를 1 증가시키고 증가된 값을 반환한다(TTL은 유지).
     */
    public long increaseAttempt(EmailPurpose purpose, String email) {
        return redisTemplate.opsForHash().increment(codeKey(purpose, email), FIELD_ATTEMPT, 1L);
    }

    /**
     * 인증 코드 키의 남은 TTL(초). 키가 없으면 0.
     */
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

    /**
     * 쿨다운 키를 NX로 설정한다. 이미 존재하면(쿨다운 중) false.
     */
    public boolean tryCooldown(EmailPurpose purpose, String email, Duration ttl) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(cooldownKey(purpose, email), "1", ttl));
    }
}
