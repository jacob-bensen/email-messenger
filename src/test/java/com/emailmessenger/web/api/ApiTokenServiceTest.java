package com.emailmessenger.web.api;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.ApiTokenRepository;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.repository.UserRepository;
import com.emailmessenger.service.ReplyService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class ApiTokenServiceTest {

    @Autowired ApiTokenService service;
    @Autowired ApiTokenRepository tokens;
    @Autowired UserService userService;
    @Autowired UserRepository users;

    @MockitoBean ReplyService replyService;
    @MockitoBean EmailThreadRepository threadRepository;
    @MockitoBean JavaMailSender mailSender;

    @BeforeEach
    void stubMimeFactory() {
        when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage((Session) null));
    }

    @Test
    void issueReturnsPrefixedPlaintextAndPersistsHash() {
        User user = userService.register("t1@example.com", "password1", null);
        var issued = service.issue(user, "test-agent");
        assertThat(issued.plaintext()).startsWith("cmxo_");
        assertThat(issued.record().getTokenHash()).hasSize(64);
        // Plaintext must never be persisted.
        assertThat(tokens.findAll())
                .allSatisfy(row -> assertThat(row.getTokenHash()).isNotEqualTo(issued.plaintext()));
    }

    @Test
    void resolveReturnsTokenAndBumpsLastUsed() {
        User user = userService.register("t2@example.com", "password1", null);
        var issued = service.issue(user, null);
        assertThat(issued.record().getLastUsedAt()).isNull();
        var resolved = service.resolve(issued.plaintext());
        assertThat(resolved).isPresent();
        assertThat(resolved.get().getLastUsedAt()).isNotNull();
    }

    @Test
    void resolveReturnsEmptyForUnknownToken() {
        assertThat(service.resolve("cmxo_no-such-token")).isEmpty();
    }

    @Test
    void revokeMakesTokenUnresolvable() {
        User user = userService.register("t3@example.com", "password1", null);
        var issued = service.issue(user, null);
        service.revoke(issued.plaintext());
        assertThat(service.resolve(issued.plaintext())).isEmpty();
    }

    @Test
    void revokeUnknownTokenIsSilentNoOp() {
        service.revoke("cmxo_unknown"); // must not throw
    }
}
