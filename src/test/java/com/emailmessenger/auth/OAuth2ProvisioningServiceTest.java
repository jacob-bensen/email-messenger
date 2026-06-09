package com.emailmessenger.auth;

import com.emailmessenger.domain.User;
import com.emailmessenger.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class OAuth2ProvisioningServiceTest {

    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;

    private final Instant fixedNow = Instant.parse("2026-06-09T12:00:00Z");
    private OAuth2ProvisioningService provisioner;

    @BeforeEach
    void buildServiceWithFixedClock() {
        provisioner = new OAuth2ProvisioningService(users, passwordEncoder,
                Clock.fixed(fixedNow, ZoneOffset.UTC));
    }

    @Test
    void brandNewGoogleEmailCreatesUserWithGoogleSourceAndVerifiedTimestamp() {
        User created = provisioner.provisionFromGoogle("Alice@Example.com", "Alice Lee", true);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getEmail()).isEqualTo("alice@example.com");
        assertThat(created.getDisplayName()).isEqualTo("Alice Lee");
        assertThat(created.getAcquisitionSource()).isEqualTo("google");
        assertThat(created.getEmailVerifiedAt())
                .isEqualTo(LocalDateTime.ofInstant(fixedNow, ZoneOffset.UTC));
        assertThat(created.getPasswordHash()).isNotBlank();
        assertThat(passwordEncoder.matches("", created.getPasswordHash())).isFalse();
    }

    @Test
    void googleSayingUnverifiedDoesNotStampEmailVerifiedAt() {
        User created = provisioner.provisionFromGoogle("unv@example.com", "U", false);

        assertThat(created.getEmailVerifiedAt()).isNull();
    }

    @Test
    void existingUserIsReturnedUntouched() {
        User original = users.save(new User("incumbent@example.com",
                passwordEncoder.encode("their-password"), "Incumbent"));
        String originalHash = original.getPasswordHash();

        User returned = provisioner.provisionFromGoogle("incumbent@example.com",
                "Different Name", true);

        assertThat(returned.getId()).isEqualTo(original.getId());
        assertThat(returned.getDisplayName()).isEqualTo("Incumbent");
        assertThat(returned.getPasswordHash()).isEqualTo(originalHash);
        assertThat(passwordEncoder.matches("their-password", returned.getPasswordHash())).isTrue();
    }

    @Test
    void existingUnverifiedUserGetsEmailVerifiedAtStampedFromGoogle() {
        User original = users.save(new User("unverified@example.com",
                passwordEncoder.encode("pw"), null));
        assertThat(original.getEmailVerifiedAt()).isNull();

        User returned = provisioner.provisionFromGoogle("unverified@example.com", null, true);

        assertThat(returned.getEmailVerifiedAt())
                .isEqualTo(LocalDateTime.ofInstant(fixedNow, ZoneOffset.UTC));
    }

    @Test
    void existingVerifiedUserKeepsOriginalVerifiedTimestamp() {
        User original = new User("already@example.com", passwordEncoder.encode("pw"), null);
        original.setEmailVerifiedAt(LocalDateTime.of(2025, 1, 1, 0, 0));
        original = users.save(original);
        LocalDateTime originalStamp = original.getEmailVerifiedAt();

        User returned = provisioner.provisionFromGoogle("already@example.com", null, true);

        assertThat(returned.getEmailVerifiedAt()).isEqualTo(originalStamp);
    }

    @Test
    void blankEmailRejected() {
        assertThatThrownBy(() -> provisioner.provisionFromGoogle("  ", "x", true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> provisioner.provisionFromGoogle(null, "x", true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blankNameStoredAsNull() {
        User created = provisioner.provisionFromGoogle("blank@example.com", "   ", true);

        assertThat(created.getDisplayName()).isNull();
    }

    @Test
    void emailNormalizedToLowercaseAndTrimmed() {
        provisioner.provisionFromGoogle("  Spaced.MIXED@Example.COM  ", "S", true);

        assertThat(users.findByEmail("spaced.mixed@example.com")).isPresent();
    }
}
