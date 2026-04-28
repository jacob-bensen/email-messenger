package com.emailmessenger.email;

import com.emailmessenger.domain.Message;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.repository.MessageRepository;
import com.emailmessenger.repository.ParticipantRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Date;
import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-stack integration tests against a real PostgreSQL container.
 * Validates that Flyway migrations, JPA mappings, and EmailImportService
 * all work correctly against PostgreSQL (not just H2).
 */
@ExtendWith(RequiresDocker.class)
@SpringBootTest
@Testcontainers
@Transactional
class EmailImportIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("emailmessenger")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void overrideDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.h2.console.enabled", () -> "false");
    }

    @Autowired private EmailImportService emailImportService;
    @Autowired private MessageRepository messageRepo;
    @Autowired private EmailThreadRepository threadRepo;
    @Autowired private ParticipantRepository participantRepo;

    private MimeMessage buildMime(String msgId, String subject, String from, String to,
                                   String body, String inReplyTo) throws Exception {
        Session session = Session.getInstance(new Properties());
        MimeMessage mime = new MimeMessage(session);
        mime.setFrom(new InternetAddress(from));
        mime.setRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(to));
        mime.setSubject(subject);
        mime.setText(body);
        mime.setHeader("Message-ID", msgId);
        if (inReplyTo != null) {
            mime.setHeader("In-Reply-To", inReplyTo);
            mime.setHeader("References", inReplyTo);
        }
        mime.setSentDate(new Date());
        return mime;
    }

    @Test
    void importNewMessage_createsThreadAndParticipantOnRealPostgres() throws Exception {
        MimeMessage mime = buildMime("<pg-msg1@test.com>", "Hello Postgres",
                "alice@example.com", "bob@example.com", "Hi Bob!", null);

        Optional<Message> result = emailImportService.importMessage(mime);

        assertThat(result).isPresent();
        assertThat(result.get().getSubject()).isEqualTo("Hello Postgres");
        assertThat(result.get().getSender().getEmail()).isEqualTo("alice@example.com");
        assertThat(result.get().getThread()).isNotNull();
        assertThat(participantRepo.findByEmail("alice@example.com")).isPresent();
    }

    @Test
    void importDuplicate_isIdempotent_onRealPostgres() throws Exception {
        MimeMessage mime = buildMime("<pg-dup@test.com>", "Duplicate Subject",
                "sender@example.com", "recv@example.com", "body text", null);

        Optional<Message> first = emailImportService.importMessage(mime);
        Optional<Message> second = emailImportService.importMessage(mime);

        assertThat(first).isPresent();
        assertThat(second).isEmpty();
    }

    @Test
    void importReply_joinsExistingThread_onRealPostgres() throws Exception {
        MimeMessage original = buildMime("<pg-orig@test.com>", "Original Thread",
                "alice@example.com", "bob@example.com", "Let's discuss", null);
        emailImportService.importMessage(original);

        MimeMessage reply = buildMime("<pg-reply@test.com>", "Re: Original Thread",
                "bob@example.com", "alice@example.com", "Sure!", "<pg-orig@test.com>");
        Optional<Message> result = emailImportService.importMessage(reply);

        assertThat(result).isPresent();
        assertThat(result.get().getThread().getRootMessageId()).isEqualTo("<pg-orig@test.com>");
        assertThat(result.get().getInReplyTo()).isEqualTo("<pg-orig@test.com>");
    }

    @Test
    void participantDeduplication_sameEmailYieldsSameRecord_onRealPostgres() throws Exception {
        MimeMessage m1 = buildMime("<pg-dedup1@test.com>", "First",
                "shared@example.com", "recv@example.com", "body1", null);
        MimeMessage m2 = buildMime("<pg-dedup2@test.com>", "Second",
                "shared@example.com", "recv@example.com", "body2", null);

        Optional<Message> r1 = emailImportService.importMessage(m1);
        Optional<Message> r2 = emailImportService.importMessage(m2);

        assertThat(r1).isPresent();
        assertThat(r2).isPresent();
        assertThat(r1.get().getSender().getId()).isEqualTo(r2.get().getSender().getId());
    }

    @Test
    void importWithReferencesHeader_resolvesThreadByReferenceChain() throws Exception {
        Session session = Session.getInstance(new Properties());

        MimeMessage root = buildMime("<pg-root-ref@test.com>", "Chain Start",
                "alice@example.com", "bob@example.com", "Starting thread", null);
        emailImportService.importMessage(root);

        MimeMessage chained = new MimeMessage(session);
        chained.setFrom(new InternetAddress("bob@example.com"));
        chained.setRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress("alice@example.com"));
        chained.setSubject("Re: Chain Start");
        chained.setText("Reply via References");
        chained.setHeader("Message-ID", "<pg-chained@test.com>");
        chained.setHeader("References", "<pg-root-ref@test.com>");
        chained.setSentDate(new Date());

        Optional<Message> result = emailImportService.importMessage(chained);

        assertThat(result).isPresent();
        assertThat(result.get().getThread().getRootMessageId()).isEqualTo("<pg-root-ref@test.com>");
    }

    @Test
    void flywayMigrationsRunCleanly_noSchemaErrors() {
        // If Flyway migrations fail on PG, this test won't even load the context.
        // This explicit assertion confirms the schema allows basic CRUD.
        var thread = threadRepo.save(
                new com.emailmessenger.domain.EmailThread("Schema test", "<schema@test.com>"));
        assertThat(thread.getId()).isNotNull();
        assertThat(thread.getCreatedAt()).isNotNull();
        assertThat(thread.getUpdatedAt()).isNotNull();
    }
}
