package com.keyfeed.keyfeedmonolithic.domain.content.repository;

import com.keyfeed.keyfeedmonolithic.domain.content.document.ContentDocument;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface ContentDocumentRepository extends ElasticsearchRepository<ContentDocument, String> {

    @Query("""
    {
      "bool": {
        "must": [
          {
            "terms": {
              "source_id": ?0
            }
          }
        ],
        "filter": [
          {
            "range": {
              "published_at": {
                "lt": "?1"
              }
            }
          }
        ]
      }
    }
    """)
    List<ContentDocument> searchBySourceIdsAndCursor(List<Long> sourceIds, String lastPublishedAt, Pageable pageable);

    @Query("""
    {
      "bool": {
        "must": [
          {
            "terms": {
              "source_id": ?0
            }
          }
        ]
      }
    }
    """)
    List<ContentDocument> searchBySourceIdsFirstPage(List<Long> sourceIds, Pageable pageable);

}