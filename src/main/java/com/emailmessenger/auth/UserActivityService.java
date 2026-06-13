package com.emailmessenger.auth;

import com.emailmessenger.domain.User;
import com.emailmessenger.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Updates the activity timestamps on {@code users} (last login, last inbox
 * visit) without loading the full entity. These columns drive the EPIC-08
 * Milestone 4 re-engagement sweep — they are written on the hot path of
 * every login and every {@code GET /threads}, so the service deliberately
 * uses {@code @Modifying} JPQL to keep each write to a single one-row
 * {@code UPDATE} and avoids bumping the entity-wide {@code updated_at}.
 */
@Service
public class UserActivityService {

    private final UserRepository users;
    private final Clock clock;

    UserActivityService(UserRepository users, Clock clock) {
        this.users = users;
        this.clock = clock;
    }

    @Transactional
    public void recordInboxVisit(User user) {
        if (user == null || user.getId() == null) {
            return;
        }
        users.touchInboxVisit(user.getId(), LocalDateTime.now(clock));
    }

    @Transactional
    public void recordLogin(String email) {
        if (email == null) {
            return;
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return;
        }
        users.touchLogin(normalized, LocalDateTime.now(clock));
    }
}
