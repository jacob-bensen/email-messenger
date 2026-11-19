package com.emailmessenger.repository;

import com.emailmessenger.domain.ApiToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ApiTokenRepository extends JpaRepository<ApiToken, Long> {

    @Query("SELECT t FROM ApiToken t JOIN FETCH t.user WHERE t.tokenHash = :hash")
    Optional<ApiToken> findWithUserByTokenHash(@Param("hash") String tokenHash);

    @Query("""
            SELECT t FROM ApiToken t
            WHERE t.user.id = :userId AND t.expiresAt > :now
            ORDER BY COALESCE(t.lastUsedAt, t.createdAt) DESC, t.id DESC
            """)
    List<ApiToken> findActiveByUserId(@Param("userId") long userId, @Param("now") LocalDateTime now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM ApiToken t WHERE t.id = :id AND t.user.id = :userId AND t.expiresAt > :now")
    int deleteActiveByIdAndUserId(@Param("id") long id, @Param("userId") long userId,
                                  @Param("now") LocalDateTime now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM ApiToken t WHERE t.user.id = :userId AND t.id <> :keepId AND t.expiresAt > :now")
    int deleteActiveByUserIdExcept(@Param("userId") long userId, @Param("keepId") long keepId,
                                   @Param("now") LocalDateTime now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM ApiToken t WHERE t.user.id = :userId AND t.expiresAt > :now")
    int deleteActiveByUserId(@Param("userId") long userId, @Param("now") LocalDateTime now);
}
