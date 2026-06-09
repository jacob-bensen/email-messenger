package com.emailmessenger.auth;

import com.emailmessenger.domain.User;
import com.emailmessenger.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Finds-or-creates a local {@link User} for a Google sign-in callback.
 *
 * <p>First "Continue with Google" click for a brand-new email creates a
 * row with a random unguessable password hash so the password-based
 * code paths (DB user-details, password-reset request) continue to
 * function — the user can later set a real password via
 * {@code /password/forgot} if they ever want a non-Google fallback.
 * The Google {@code email_verified} claim short-circuits our own
 * verification flow because Google has already proved the address.
 *
 * <p>For an existing email-password user who clicks "Continue with
 * Google", the row is returned untouched aside from a one-time
 * {@code email_verified_at} stamp if the address wasn't already
 * verified — never overwriting the existing password hash or display
 * name.
 */
@Service
public class OAuth2ProvisioningService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    OAuth2ProvisioningService(UserRepository users,
                              PasswordEncoder passwordEncoder,
                              Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public User provisionFromGoogle(String email, String displayName, boolean emailVerified) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Google sign-in returned no email");
        }
        String normalized = UserService.normalizeEmail(email);
        User existing = users.findByEmail(normalized).orElse(null);
        LocalDateTime now = LocalDateTime.now(clock);
        if (existing != null) {
            if (emailVerified && existing.getEmailVerifiedAt() == null) {
                existing.setEmailVerifiedAt(now);
                users.save(existing);
            }
            return existing;
        }
        String trimmedName = (displayName == null || displayName.isBlank()) ? null : displayName.trim();
        User fresh = new User(normalized, passwordEncoder.encode(randomSecret()), trimmedName);
        fresh.setAcquisitionSource("google");
        if (emailVerified) {
            fresh.setEmailVerifiedAt(now);
        }
        return users.save(fresh);
    }

    private String randomSecret() {
        byte[] buf = new byte[32];
        random.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}
