package com.emailmessenger.auth;

import com.emailmessenger.domain.AuthEvent;
import com.emailmessenger.domain.AuthEventType;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.AuthEventRepository;
import com.emailmessenger.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Writes one row per authentication-shaped event (logins, password and
 * email changes, password resets, throttle lockouts) into
 * {@code auth_events}, and reads the most-recent N for the per-user
 * "recent account activity" panel on {@code /account}.
 *
 * <p>The email is normalized (trim + lowercase) for every write so the
 * throttle counts and the activity panel both see a single canonical
 * key regardless of the casing the user typed at the form.
 */
@Service
public class AuthEventService {

    private final AuthEventRepository events;
    private final UserRepository users;

    AuthEventService(AuthEventRepository events, UserRepository users) {
        this.events = events;
        this.users = users;
    }

    @Transactional
    public void record(User user, String email, AuthEventType type, String ipAddress) {
        String normalizedEmail = normalize(email);
        if (normalizedEmail == null) {
            return;
        }
        User resolved = user;
        if (resolved == null) {
            resolved = users.findByEmail(normalizedEmail).orElse(null);
        }
        events.save(new AuthEvent(resolved, normalizedEmail, type, ipAddress));
    }

    @Transactional
    public void recordForEmail(String email, AuthEventType type, String ipAddress) {
        record(null, email, type, ipAddress);
    }

    @Transactional(readOnly = true)
    public List<AuthEvent> recentFor(User user, int limit) {
        if (user == null || user.getId() == null) {
            return List.of();
        }
        return events.findByUserOrderByCreatedAtDesc(user, PageRequest.of(0, limit));
    }

    static String normalize(String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim().toLowerCase(java.util.Locale.ROOT);
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() > 254 ? trimmed.substring(0, 254) : trimmed;
    }

    static Optional<String> safeIp(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String trimmed = raw.trim();
        if (trimmed.length() > 45) {
            trimmed = trimmed.substring(0, 45);
        }
        return Optional.of(trimmed);
    }
}
