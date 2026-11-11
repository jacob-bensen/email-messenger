package com.emailmessenger.repository;

import com.emailmessenger.domain.ApiToken;
import com.emailmessenger.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ApiTokenRepository extends JpaRepository<ApiToken, Long> {

    @Query("SELECT t FROM ApiToken t JOIN FETCH t.user WHERE t.tokenHash = :hash")
    Optional<ApiToken> findByTokenHash(@Param("hash") String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE ApiToken t SET t.revokedAt = :ts " +
            "WHERE t.user = :user AND t.revokedAt IS NULL")
    int revokeAllFor(@Param("user") User user, @Param("ts") LocalDateTime ts);
}
