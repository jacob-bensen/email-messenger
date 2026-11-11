package com.emailmessenger.auth;

import com.emailmessenger.domain.PasswordResetToken;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.ApiTokenRepository;
import com.emailmessenger.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ApiTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-11T10:00:00Z");

    @Autowired ApiTokenRepository tokens;
    @Autowired UserService userService;
    @Autowired AccountService accountService;
    @Autowired PasswordResetService passwordResetService;
    @Autowired PasswordResetTokenRepository resetTokens;

    private User user;
    private ApiTokenService service;

    @BeforeEach
    void setUp() {
        user = userService.register("token-user@example.com", "password1", "Token User");
        service = serviceAt(NOW);
    }

    private ApiTokenService serviceAt(Instant instant) {
        return new ApiTokenService(tokens, Clock.fixed(instant, ZoneOffset.UTC), 90);
    }

    @Test
    void issuedTokenAuthenticatesItsUser() {
        ApiTokenService.IssuedToken issued = service.issue(user, "Pixel 9");

        assertThat(issued.token()).startsWith("cmx_");
        assertThat(service.authenticate(issued.token())).containsSame(user);
    }

    @Test
    void onlyTheHashIsStored() {
        ApiTokenService.IssuedToken issued = service.issue(user, null);

        assertThat(tokens.findAll()).singleElement().satisfies(t -> {
            assertThat(t.getTokenHash()).isEqualTo(ApiTokenService.hash(issued.token()));
            assertThat(t.getTokenHash()).doesNotContain(issued.token());
            assertThat(t.getDeviceName()).isNull();
        });
    }

    @Test
    void unknownAndMalformedTokensAreRejected() {
        service.issue(user, null);

        assertThat(service.authenticate(null)).isEmpty();
        assertThat(service.authenticate("")).isEmpty();
        assertThat(service.authenticate("cmx_not-a-real-token")).isEmpty();
    }

    @Test
    void revokedTokenStopsAuthenticating() {
        String token = service.issue(user, null).token();

        service.revoke(token);
        service.revoke(token);

        assertThat(service.authenticate(token)).isEmpty();
    }

    @Test
    void tokenExpiresAfterItsTtl() {
        String token = service.issue(user, null).token();

        assertThat(serviceAt(NOW.plus(Duration.ofDays(89))).authenticate(token)).isPresent();
        assertThat(serviceAt(NOW.plus(Duration.ofDays(90))).authenticate(token)).isEmpty();
    }

    @Test
    void disabledUsersTokenIsRejected() {
        String token = service.issue(user, null).token();

        user.setEnabled(false);

        assertThat(service.authenticate(token)).isEmpty();
    }

    @Test
    void changingThePasswordRevokesEveryTokenOfThatUserOnly() {
        User other = userService.register("other-token-user@example.com", "password1", null);
        String phone = service.issue(user, "phone").token();
        String tablet = service.issue(user, "tablet").token();
        String theirs = service.issue(other, null).token();

        assertThat(accountService.changePassword(user, "password1", "new-password1"))
                .isEqualTo(AccountService.PasswordChangeOutcome.OK);

        assertThat(service.authenticate(phone)).isEmpty();
        assertThat(service.authenticate(tablet)).isEmpty();
        assertThat(service.authenticate(theirs)).isPresent();
    }

    @Test
    void completingAPasswordResetRevokesTokens() {
        String token = service.issue(user, null).token();
        PasswordResetToken reset = resetTokens.save(new PasswordResetToken(
                user, PasswordResetService.sha256Hex("plain-reset-token"),
                LocalDateTime.now(ZoneOffset.UTC).plusHours(1)));
        assertThat(reset.getId()).isNotNull();

        assertThat(passwordResetService.consumeToken("plain-reset-token", "new-password1"))
                .isPresent();

        assertThat(service.authenticate(token)).isEmpty();
    }

    @Test
    void longDeviceNameIsTruncated() {
        service.issue(user, "x".repeat(150));

        assertThat(tokens.findAll()).singleElement()
                .satisfies(t -> assertThat(t.getDeviceName()).hasSize(100));
    }
}
