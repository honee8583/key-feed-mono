package com.keyfeed.keyfeedmonolithic.dummy;

import com.keyfeed.keyfeedmonolithic.domain.auth.entity.User;
import com.keyfeed.keyfeedmonolithic.domain.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@ActiveProfiles("local")
@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.jdbc.batch_size=1000",
        "spring.jpa.properties.hibernate.order_inserts=true"
})
class KeywordBulkInsertTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    private static final List<String> KEYWORD_POOL = List.of(
            "spring", "kafka", "스프링", "카프카",
            "k8s", "쿠버네티스", "MSA", "분산",
            "db", "데이터베이스", "redis", "레디스"
    );

    private static final int PAGE_SIZE = 10_000; // 한 번에 처리할 유저 수

    @Test
    void insertKeywordsBulk() {
        Random random = new Random();
        int pageNumber = 0;
        Page<User> page;

        do {
            // 10,000명씩 페이징해서 조회
            page = userRepository.findAll(PageRequest.of(pageNumber, PAGE_SIZE));
            List<Long> userIds = page.getContent()
                    .stream()
                    .map(User::getId)
                    .toList();

            List<Object[]> batchArgs = new ArrayList<>();

            for (Long userId : userIds) {
                List<String> shuffled = new ArrayList<>(KEYWORD_POOL);
                Collections.shuffle(shuffled);

                int count = random.nextInt(8); // 0~7
                List<String> selected = shuffled.subList(0, count);

                for (String keyword : selected) {
                    batchArgs.add(new Object[]{userId, keyword, random.nextInt(2)});
                }
            }

            if (!batchArgs.isEmpty()) {
                jdbcTemplate.batchUpdate(
                        "INSERT INTO keyword (user_id, name, is_notification_enabled) VALUES (?, ?, ?)",
                        batchArgs
                );
            }

            System.out.println("페이지 " + pageNumber + " 완료 / 전체 " + page.getTotalPages() + " 페이지");
            pageNumber++;

        } while (page.hasNext());

        System.out.println("삽입 완료");
    }
}
