package com.keyfeed.keyfeedmonolithic.domain.content.repository;

import com.keyfeed.keyfeedmonolithic.domain.content.entity.Content;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContentRepository extends JpaRepository<Content, Long> {

    @Query("SELECT c FROM Content c WHERE c.sourceId IN :sourceIds ORDER BY c.id DESC")
    List<Content> findFirstPage(@Param("sourceIds") List<Long> sourceIds, Pageable pageable);

    @Query("SELECT c FROM Content c WHERE c.sourceId IN :sourceIds AND c.id < :lastId ORDER BY c.id DESC")
    List<Content> findNextPage(@Param("sourceIds") List<Long> sourceIds, @Param("lastId") Long lastId, Pageable pageable);

    @Query("SELECT c FROM Content c WHERE c.sourceId IN :sourceIds AND (c.title LIKE %:keyword% OR c.summary LIKE %:keyword%) ORDER BY c.id DESC")
    List<Content> searchFirstPage(@Param("sourceIds") List<Long> sourceIds, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT c FROM Content c WHERE c.sourceId IN :sourceIds AND c.id < :lastId AND (c.title LIKE %:keyword% OR c.summary LIKE %:keyword%) ORDER BY c.id DESC")
    List<Content> searchNextPage(@Param("sourceIds") List<Long> sourceIds, @Param("lastId") Long lastId, @Param("keyword") String keyword, Pageable pageable);
}
