//package com.keyfeed.keyfeedmonolithic.domain.crawl.scheduler;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.ApplicationArguments;
//import org.springframework.boot.ApplicationRunner;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class KeywordCacheInitializer implements ApplicationRunner {
//
//    private final StringRedisTemplate redisTemplate;
//    private final JdbcTemplate jdbcTemplate;
//
//    private static final String KEY_PREFIX = "keyword:users:";
//
//    @Override
//    public void run(ApplicationArguments args) {
//        // Redis에 데이터가 이미 있으면 동기화 생략
//        Set<String> existingKeys = redisTemplate.keys(KEY_PREFIX + "*");
//        if (!existingKeys.isEmpty()) {
//            log.info("Redis 키워드 캐시 이미 존재 - 동기화 생략 ({}개 키워드)", existingKeys.size());
//            return;
//        }
//
//        log.info("Redis 키워드 캐시 초기화 시작");
//        long start = System.currentTimeMillis();
//
//        syncAllKeywords();
//
//        log.info("Redis 키워드 캐시 초기화 완료 - {}ms", System.currentTimeMillis() - start);
//    }
//
//    private void syncAllKeywords() {
//        int pageSize = 10_000;
//        long lastId = 0;
//        int totalCount = 0;
//
//        while (true) {
//            // cursor 페이징으로 keyword 테이블 전체 읽기
//            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
//                    "SELECT keyword_id, user_id, name " +
//                            "FROM keyword " +
//                            "WHERE is_notification_enabled = 1 " +
//                            "AND keyword_id > ? " +
//                            "ORDER BY keyword_id " +
//                            "LIMIT ?",
//                    lastId, pageSize
//            );
//
//            if (rows.isEmpty()) break;
//
//            // 키워드별로 그룹핑 후 Redis에 저장
//            Map<String, List<String>> grouped = rows.stream()
//                    .collect(Collectors.groupingBy(
//                            row -> (String) row.get("name"),
//                            Collectors.mapping(
//                                    row -> String.valueOf(row.get("user_id")),
//                                    Collectors.toList()
//                            )
//                    ));
//
//            grouped.forEach((keyword, userIds) -> {
//                String redisKey = KEY_PREFIX + keyword;
//                redisTemplate.opsForSet().add(redisKey, userIds.toArray(new String[0]));
//            });
//
//            totalCount += rows.size();
//            lastId = (Long) rows.get(rows.size() - 1).get("keyword_id");
//            log.info("Redis 동기화 진행 중 - {}건 처리", totalCount);
//
//            if (rows.size() < pageSize) break;
//        }
//
//        log.info("Redis 동기화 완료 - 총 {}건", totalCount);
//    }
//}