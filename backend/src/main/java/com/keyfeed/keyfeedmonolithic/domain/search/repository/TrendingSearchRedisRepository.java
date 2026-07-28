package com.keyfeed.keyfeedmonolithic.domain.search.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

@Repository
@RequiredArgsConstructor
public class TrendingSearchRedisRepository {

    private final StringRedisTemplate redisTemplate;

    private static final String BUCKET_PREFIX = "search:trending:";
    private static final String DEDUP_PREFIX = "search:trending:dedup:";
    private static final String CACHE_KEY = "search:trending:cache";
    private static final DateTimeFormatter BUCKET_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHH");
    private static final int WINDOW_HOURS = 24;
    private static final Duration BUCKET_TTL = Duration.ofHours(25);
    private static final Duration CACHE_TTL = Duration.ofMinutes(1);
    private static final int MAX_COUNT = 10;

    public void increment(Long userId, String keyword) {
        String bucket = LocalDateTime.now().format(BUCKET_FORMATTER);
        String dedupKey = DEDUP_PREFIX + bucket;

        Long added = redisTemplate.opsForSet().add(dedupKey, userId + ":" + keyword);
        if (added == null || added == 0) {
            return;
        }
        redisTemplate.expire(dedupKey, BUCKET_TTL);

        String bucketKey = BUCKET_PREFIX + bucket;
        redisTemplate.opsForZSet().incrementScore(bucketKey, keyword, 1);
        redisTemplate.expire(bucketKey, BUCKET_TTL);
    }

    public List<TypedTuple<String>> findTopWithScores() {
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(CACHE_KEY))) {
            aggregateWindowIntoCache();
        }

        Set<TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(CACHE_KEY, 0, MAX_COUNT - 1);
        if (tuples == null) {
            return List.of();
        }
        return new ArrayList<>(tuples);
    }

    private void aggregateWindowIntoCache() {
        LocalDateTime now = LocalDateTime.now();
        List<String> bucketKeys = IntStream.range(0, WINDOW_HOURS)
                .mapToObj(hoursAgo -> BUCKET_PREFIX + now.minusHours(hoursAgo).format(BUCKET_FORMATTER))
                .toList();

        redisTemplate.opsForZSet()
                .unionAndStore(bucketKeys.get(0), bucketKeys.subList(1, bucketKeys.size()), CACHE_KEY);
        redisTemplate.expire(CACHE_KEY, CACHE_TTL);
    }
}
