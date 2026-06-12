package com.emailmessenger.repository;

import com.emailmessenger.domain.PasswordResetToken;
import com.emailmessenger.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE PasswordResetToken t SET t.usedAt = :ts " +
            "WHERE t.user = :user AND t.usedAt IS NULL")
    int markAllUsedFor(@Param("user") User user, @Param("ts") LocalDateTime ts);
}
