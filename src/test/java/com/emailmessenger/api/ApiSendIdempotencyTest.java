package com.emailmessenger.api;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.repository.ApiSendRequestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** docs/API.md §5.8.1 key lifetime and states, against the real table. */
@SpringBootTest
@Transactional
class ApiSendIdempotencyTest {

    private static final Instant T0 = Instant.parse("2026-11-19T10:00:00Z");

    @Autowired ApiSendRequestRepository requests;
    @Autowired UserService userService;

    private ApiSendIdempotency at(Instant now) {
        return new ApiSendIdempotency(requests, Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void aKeyIsForgottenAfterTwentyFourHours() {
        long userId = userService.register("ada@example.com", "correct-horse", "Ada").getId();
        var first = at(T0).claim(userId, "k", "hash-a");
        at(T0).complete(first.id(), new ApiDtos.SendResponse("key", 1, 2));

        assertThatThrownBy(() -> at(T0.plusSeconds(23 * 3600)).claim(userId, "k", "hash-b"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("idempotency_key_reused"));

        var later = at(T0.plusSeconds(25 * 3600)).claim(userId, "k", "hash-b");
        assertThat(later.replay()).isNull();
        assertThat(later.id()).isNotEqualTo(first.id());
    }

    @Test
    void keysAreScopedPerUser() {
        long ada = userService.register("ada@example.com", "correct-horse", "Ada").getId();
        long bob = userService.register("bob@example.com", "correct-horse", "Bob").getId();
        var adas = at(T0).claim(ada, "same", "hash-a");
        at(T0).complete(adas.id(), new ApiDtos.SendResponse("key", 1, 2));

        assertThat(at(T0).claim(bob, "same", "hash-b").replay()).isNull();
        assertThat(at(T0).claim(ada, "same", "hash-a").replay())
                .isEqualTo(new ApiDtos.SendResponse("key", 1, 2));
    }

    @Test
    void validatesTheKeyAlphabetAndLength() {
        ApiSendIdempotency.validate("6f1c2a7e-3b0d-4c5e-9a8f-1d2e3c4b5a69");
        ApiSendIdempotency.validate("a.b_c:d");
        assertThatThrownBy(() -> ApiSendIdempotency.validate("")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> ApiSendIdempotency.validate("ü")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> ApiSendIdempotency.validate("x".repeat(101))).isInstanceOf(ApiException.class);
    }
}
