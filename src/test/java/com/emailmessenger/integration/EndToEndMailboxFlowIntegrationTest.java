package com.emailmessenger.integration;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.repository.UserRepository;
import com.emailmessenger.service.ReplyService;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Real-stack happy path on the revenue critical path: a user registers,
 * connects an IMAP mailbox served by GreenMail, and sees the imported
 * thread on /threads — backed by a Postgres 16 container with the
 * Flyway-migrated schema. Gates CI against regressions on the connect-
 * mailbox → import → render loop that every paying user has to traverse.
 *
 * <p>Skipped automatically when the host has no Docker daemon so local
 * developers without Docker still get a green build; CI always has one.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("dev")
@EnabledIf("com.emailmessenger.integration.EndToEndMailboxFlowIntegrationTest#dockerAvailable")
class EndToEndMailboxFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.IMAP);

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
        // H2 is on the test classpath; make sure its console isn't wired
        // against the Postgres datasource.
        registry.add("spring.h2.console.enabled", () -> "false");
    }

    static boolean dockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            return false;
        }
    }

    @Autowired MockMvc mockMvc;
    @Autowired UserService userService;
    @Autowired UserRepository userRepository;
    @Autowired EmailThreadRepository threadRepository;

    // SMTP isn't reachable in tests; the reply path is out of scope here.
    @MockBean ReplyService replyService;

    @Test
    void connectMailboxImportsThreadOverGreenMailAndShowsItOnThreadsView() throws Exception {
        String mailboxEmail = "alice@local.test";
        String mailboxPw = "imap-secret";
        GreenMailUser inbox = greenMail.setUser(mailboxEmail, mailboxPw);

        MimeMessage incoming = GreenMailUtil.createTextEmail(
                mailboxEmail,
                "boss@acme.com",
                "Project kickoff",
                "Hi Alice — let's chat about the kickoff.",
                ServerSetupTest.IMAP);
        incoming.setHeader("Message-ID", "<integration-test-msg@local.test>");
        inbox.deliver(incoming);

        userService.register("integration@example.com", "password1", "Integration");

        mockMvc.perform(post("/mailboxes")
                        .with(user("integration@example.com"))
                        .with(csrf())
                        .param("host", "127.0.0.1")
                        .param("port", String.valueOf(ServerSetupTest.IMAP.getPort()))
                        .param("ssl", "false")
                        .param("username", mailboxEmail)
                        .param("password", mailboxPw))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/threads"));

        mockMvc.perform(get("/threads")
                        .with(user("integration@example.com")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Project kickoff")));

        User owner = userRepository.findByEmail("integration@example.com").orElseThrow();
        assertThat(threadRepository.findByOwnerOrderByUpdatedAtDesc(owner, PageRequest.of(0, 10))
                .getContent())
                .extracting(EmailThread::getSubject)
                .contains("Project kickoff");
    }
}
