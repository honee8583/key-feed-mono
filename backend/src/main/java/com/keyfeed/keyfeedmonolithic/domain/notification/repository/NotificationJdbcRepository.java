package com.keyfeed.keyfeedmonolithic.domain.notification.repository;

import com.keyfeed.keyfeedmonolithic.domain.notification.dto.ContentEventPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class NotificationJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public void bulkInsertNotifications(List<Long> userIds,
                                        Map<Long, Set<String>> userMatchedKeywords,
                                        ContentEventPayload content) {

        String sql = """
            INSERT INTO notification (user_id, content_id, original_url, title, message, is_read, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, false, NOW(), NOW())
        """;

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Long userId = userIds.get(i);
                Set<String> matched = userMatchedKeywords.get(userId);

                String keywordStr = String.join(", ", matched);
                String message = matched.size() == 1
                        ? keywordStr + " 관련 새 글이 올라왔습니다."
                        : keywordStr + " 등 관련 새 글이 올라왔습니다.";

                ps.setLong(1, userId);
                ps.setLong(2, content.getContentId());
                ps.setString(3, content.getOriginalUrl());
                ps.setString(4, content.getTitle());
                ps.setString(5, message);
            }

            @Override
            public int getBatchSize() {
                return userIds.size();
            }
        });
    }
}
