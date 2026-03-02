package com.keyfeed.keyfeedmonolithic.domain.content.document;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "contents", createIndex = true)
@Setting(settingPath = "/elasticsearch/settings.json")
public class ContentDocument {

    @Id
    private String id;

    @Field(name = "content_id", type = FieldType.Long)
    private Long contentId;

    @Field(name = "source_id", type = FieldType.Long)
    private Long sourceId;

    @Field(name = "source_name", type = FieldType.Keyword)
    private String sourceName;

    @Field(type = FieldType.Text, analyzer = "korean_analyzer")
    private String title;

    @Field(type = FieldType.Text, analyzer = "korean_analyzer")
    private String summary;

    @Field(name = "original_url", type = FieldType.Keyword, index = false)
    private String originalUrl;

    @Field(name = "thumbnail_url", type = FieldType.Keyword, index = false)
    private String thumbnailUrl;

    @Field(name = "published_at", type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime publishedAt;

    @Field(name = "created_at", type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime createdAt;

}