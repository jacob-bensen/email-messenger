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
 * name. The Google OIDC {@code sub} is written onto the row on first
 * link so subsequent sign-ins survive a Google-side email change.
 */
@Service
public class OAuth2ProvisioningService {

    static final String DEFAULT_SOURCE = "google";
    private static final int SOURCE_MAX = 64;
    private static final int SUBJECT_MAX = 255;

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
        return provisionFromGoogle(email, displayName, emailVerified, null, null);
    }

    @Transactional
    public User provisionFromGoogle(String email,
                                    String displayName,
                                    boolean emailVerified,
                                    String acquisitionSource) {
        return provisionFromGoogle(email, displayName, emailVerified, acquisitionSource, null);
    }

    /**
     * @param acquisitionSource utm_source (or other inbound channel tag)
     *        captured before the OAuth round-trip — drives EPIC-12's
     *        funnel attribution. Null/blank falls back to
     *        {@value #DEFAULT_SOURCE} so a direct Google sign-up is
     *        still credited to the channel. Only used on first
     *        provision; an existing row's source is never overwritten.
     * @param googleSubject the OIDC {@code sub} claim — Google's stable
     *        per-account id. Preferred over email match on lookup so a
     *        rename of the Google address still resolves to the same
     *        MailIM row. Written onto an email-matched row on first
     *        OAuth login (account-linking).
     */
    @Transactional
    public User provisionFromGoogle(String email,
                                    String displayName,
                                    boolean emailVerified,
                                    String acquisitionSource,
                                    String googleSubject) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Google sign-in returned no email");
        }
        String normalized = UserService.normalizeEmail(email);
        String trimmedSubject = trimSubject(googleSubject);
        LocalDateTime now = LocalDateTime.now(clock);

        User existing = null;
        if (trimmedSubject != null) {
            existing = users.findByGoogleSubject(trimmedSubject).orElse(null);
        }
        if (existing == null) {
            existing = users.findByEmail(normalized).orElse(null);
        }
        if (existing != null) {
            boolean dirty = false;
            if (emailVerified && existing.getEmailVerifiedAt() == null) {
                existing.setEmailVerifiedAt(now);
                dirty = true;
            }
            if (trimmedSubject != null && existing.getGoogleSubject() == null) {
                existing.setGoogleSubject(trimmedSubject);
                dirty = true;
            }
            if (dirty) {
                users.save(existing);
            }
            return existing;
        }
        String trimmedName = (displayName == null || displayName.isBlank()) ? null : displayName.trim();
        User fresh = new User(normalized, passwordEncoder.encode(randomSecret()), trimmedName);
        fresh.setAcquisitionSource(normalizeSource(acquisitionSource));
        fresh.setGoogleSubject(trimmedSubject);
        if (emailVerified) {
            fresh.setEmailVerifiedAt(now);
        }
        return users.save(fresh);
    }

    private static String normalizeSource(String raw) {
        if (raw == null) {
            return DEFAULT_SOURCE;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return DEFAULT_SOURCE;
        }
        return trimmed.length() > SOURCE_MAX ? trimmed.substring(0, SOURCE_MAX) : trimmed;
    }

    private static String trimSubject(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() > SUBJECT_MAX ? trimmed.substring(0, SUBJECT_MAX) : trimmed;
    }

    private String randomSecret() {
        byte[] buf = new byte[32];
        random.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}
