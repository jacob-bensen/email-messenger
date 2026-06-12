package com.emailmessenger.auth;

import com.emailmessenger.domain.User;
import com.emailmessenger.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    UserService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(String email, String rawPassword, String displayName) {
        return register(email, rawPassword, displayName, null);
    }

    @Transactional
    public User register(String email, String rawPassword, String displayName, String acquisitionSource) {
        String normalized = normalizeEmail(email);
        if (users.existsByEmail(normalized)) {
            throw new EmailAlreadyRegisteredException(normalized);
        }
        String trimmedName = (displayName == null || displayName.isBlank()) ? null : displayName.trim();
        User user = new User(normalized, passwordEncoder.encode(rawPassword), trimmedName);
        user.setAcquisitionSource(normalizeSource(acquisitionSource));
        return users.save(user);
    }

    private static String normalizeSource(String source) {
        if (source == null) {
            return null;
        }
        String trimmed = source.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() > 64 ? trimmed.substring(0, 64) : trimmed;
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return users.findByEmail(normalizeEmail(email));
    }

    @Transactional(readOnly = true)
    public User requireByEmail(String email) {
        return users.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated principal has no User record: " + email));
    }

    static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
