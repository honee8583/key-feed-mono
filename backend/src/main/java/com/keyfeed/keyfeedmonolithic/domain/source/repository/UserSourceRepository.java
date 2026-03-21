package com.keyfeed.keyfeedmonolithic.domain.source.repository;

import com.keyfeed.keyfeedmonolithic.domain.source.entity.UserSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserSourceRepository extends JpaRepository<UserSource, Long> {

    List<UserSource> findByUserId(Long userId);

    List<UserSource> findByUserIdAndReceiveFeedTrue(Long userId);

    Optional<UserSource> findByIdAndUserId(Long userSourceId, Long userId);

    boolean existsByUserIdAndSourceId(Long userId, Long sourceId);

    @Query("""
        SELECT us FROM UserSource us JOIN FETCH us.source s
        WHERE us.user.id = :userId
        AND (LOWER(us.userDefinedName) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(s.url) LIKE LOWER(CONCAT('%', :keyword, '%')))
    """)
    List<UserSource> searchByUserIdAndKeyword(@Param("userId") Long userId, @Param("keyword") String keyword);

    @Modifying
    @Query("DELETE FROM UserSource us WHERE us.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

}
