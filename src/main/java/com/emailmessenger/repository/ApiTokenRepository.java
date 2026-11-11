package com.emailmessenger.repository;

import com.emailmessenger.domain.ApiToken;
import com.emailmessenger.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApiTokenRepository extends JpaRepository<ApiToken, Long> {

    @Query("SELECT t FROM ApiToken t JOIN FETCH t.user WHERE t.tokenHash = :hash")
    Optional<ApiToken> findByTokenHashWithUser(@Param("hash") String hash);

    List<ApiToken> findByUserAndRevokedAtIsNull(User user);
}
