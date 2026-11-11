package com.emailmessenger.auth;

import com.emailmessenger.domain.AuthEventType;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Password login for the mobile client. It deliberately mirrors the web
 * form-login rules rather than inventing its own: the same
 * {@link LoginThrottleService} lock (checked before the password, so a locked
 * account can't be probed), the same {@code auth_events} rows
 * ({@code LOGIN_FAILURE} / {@code ACCOUNT_LOCKED} / {@code LOGIN_SUCCESS}) —
 * which means web and API failures count against one shared budget — and the
 * same {@code last_login_at} stamp.
 *
 * <p>Not {@code @Transactional}: the failure rows must commit even though the
 * method then throws.
 */
@Service
public class ApiLoginService {

    public record LoginResult(User user, ApiTokenService.IssuedToken token) {}

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final LoginThrottleService throttle;
    private final AuthEventService authEvents;
    private final UserActivityService activity;
    private final ApiTokenService tokens;
    // Compared against when the email is unknown, so "no such user" costs the
    // same bcrypt work as "wrong password" and can't be told apart by timing.
    private final String decoyHash;

    ApiLoginService(UserRepository users,
                    PasswordEncoder passwordEncoder,
                    LoginThrottleService throttle,
                    AuthEventService authEvents,
                    UserActivityService activity,
                    ApiTokenService tokens) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.throttle = throttle;
        this.authEvents = authEvents;
        this.activity = activity;
        this.tokens = tokens;
        this.decoyHash = passwordEncoder.encode("decoy-password-never-matches");
    }

    /**
     * @throws LockedException         too many recent failures for this email or IP
     * @throws BadCredentialsException unknown email, wrong password, disabled
     *                                 account, or a Google-only account with no password
     */
    public LoginResult login(String email, String rawPassword, String deviceName) {
        String normalized = UserService.normalizeEmail(email);
        String ip = ClientIp.fromCurrentRequest();
        if (throttle.isLocked(normalized, ip)) {
            throw new LockedException("Too many failed sign-in attempts");
        }

        Optional<User> found = users.findByEmail(normalized);
        String hash = found.map(User::getPasswordHash).orElse(decoyHash);
        boolean passwordMatches = passwordEncoder.matches(rawPassword, hash);
        boolean allowed = found.isPresent() && passwordMatches
                && found.get().isEnabled() && found.get().isPasswordSet();
        if (!allowed) {
            authEvents.recordForEmail(normalized, AuthEventType.LOGIN_FAILURE, ip);
            if (throttle.isLocked(normalized, ip)) {
                authEvents.recordForEmail(normalized, AuthEventType.ACCOUNT_LOCKED, ip);
            }
            throw new BadCredentialsException("Invalid email or password");
        }

        User user = found.get();
        activity.recordLogin(user.getEmail());
        authEvents.record(user, user.getEmail(), AuthEventType.LOGIN_SUCCESS, ip);
        return new LoginResult(user, tokens.issue(user, deviceName));
    }
}
