package com.emailmessenger.auth;

import com.emailmessenger.domain.AuthEventType;
import com.emailmessenger.repository.AuthEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Counts recent {@link AuthEventType#LOGIN_FAILURE} rows per email and per
 * IP within a sliding window and returns whether further attempts must
 * be blocked. The lock is purely derived from the event log — there's
 * no separate locked-flag column to keep in sync, so a successful login
 * after the window quietly drops the lock without any explicit reset.
 *
 * <p>Defaults: 5 failures in 15 minutes for the same email OR the same
 * IP. Both keys count independently so a single attacker spraying many
 * emails from one IP still trips the IP cap, and a credential-stuffing
 * pass against one account from rotating IPs still trips the email cap.
 */
@Service
public class LoginThrottleService {

    private final AuthEventRepository events;
    private final Clock clock;
    private final int maxFailures;
    private final Duration window;

    LoginThrottleService(AuthEventRepository events,
                         Clock clock,
                         @Value("${auth.throttle.max-failures:5}") int maxFailures,
                         @Value("${auth.throttle.window-minutes:15}") int windowMinutes) {
        this.events = events;
        this.clock = clock;
        this.maxFailures = Math.max(1, maxFailures);
        this.window = Duration.ofMinutes(Math.max(1, windowMinutes));
    }

    @Transactional(readOnly = true)
    public boolean isLocked(String email, String ipAddress) {
        LocalDateTime since = LocalDateTime.now(clock).minus(window);
        String normalizedEmail = AuthEventService.normalize(email);
        if (normalizedEmail != null) {
            long emailFailures = events.countRecentForEmail(
                    AuthEventType.LOGIN_FAILURE, normalizedEmail, since);
            if (emailFailures >= maxFailures) {
                return true;
            }
        }
        if (ipAddress != null && !ipAddress.isBlank()) {
            long ipFailures = events.countRecentForIp(
                    AuthEventType.LOGIN_FAILURE, ipAddress.trim(), since);
            return ipFailures >= maxFailures;
        }
        return false;
    }

    public int getMaxFailures() {
        return maxFailures;
    }

    public Duration getWindow() {
        return window;
    }
}
