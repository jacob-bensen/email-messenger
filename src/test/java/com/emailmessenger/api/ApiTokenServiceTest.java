package com.emailmessenger.api;

import com.emailmessenger.domain.ApiToken;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.ApiTokenRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-15T12:00:00Z");
    private static final LocalDateTime NOW_LOCAL = LocalDateTime.ofInstant(NOW, ZoneOffset.UTC);

    private final ApiTokenRepository repo = mock(ApiTokenRepository.class);
    private final ApiTokenService svc = new ApiTokenService(repo, Clock.fixed(NOW, ZoneOffset.UTC));
    private final User user = new User("ada@example.com", "h", "Ada");

    @Test
    void issueStoresOnlyTheHashAndExpiresIn30Days() {
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var issued = svc.issue(user, "  " + "x".repeat(150));

        ArgumentCaptor<ApiToken> saved = ArgumentCaptor.forClass(ApiToken.class);
        verify(repo).save(saved.capture());
        assertThat(saved.getValue().getTokenHash()).isEqualTo(ApiTokenService.hash(issued.rawToken()));
        assertThat(saved.getValue().getTokenHash()).isNotEqualTo(issued.rawToken());
        assertThat(saved.getValue().getDeviceName()).hasSize(ApiTokenService.MAX_DEVICE_NAME_LENGTH);
        assertThat(issued.expiresAt()).isEqualTo(NOW_LOCAL.plusDays(30));
    }

    @Test
    void expiredTokenIsRejected() {
        ApiToken token = new ApiToken(user, "h", null, NOW_LOCAL.minusDays(31), NOW_LOCAL);
        when(repo.findWithUserByTokenHash(ApiTokenService.hash("raw"))).thenReturn(Optional.of(token));

        assertThat(svc.authenticate("raw")).isEmpty();
    }

    @Test
    void blankTokenIsRejectedWithoutALookup() {
        assertThat(svc.authenticate("  ")).isEmpty();
    }
}
