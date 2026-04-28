package com.emailmessenger.email;

import com.emailmessenger.domain.Message;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;

import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests using GreenMail as an in-process SMTP/IMAP server.
 * Validates that emails sent via SMTP and retrieved via IMAP are correctly
 * parsed and imported by EmailImportService.
 */
@SpringBootTest
@Transactional
class GreenMailSmtpImapIntegrationTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP_IMAP)
            .withConfiguration(GreenMailConfiguration.aConfig()
                    .withUser("inbox@example.com", "secret"));

    @Autowired private EmailImportService emailImportService;

    @Test
    void emailSentViaSmtp_parsedCorrectlyOnImport() throws Exception {
        GreenMailUtil.sendTextEmailTest(
                "inbox@example.com",
                "sender@external.com",
                "Hello from SMTP",
                "This is a test email body.");

        assertThat(greenMail.waitForIncomingEmail(5000, 1)).isTrue();
        MimeMessage[] received = greenMail.getReceivedMessages();

        Optional<Message> imported = emailImportService.importMessage(received[0]);

        assertThat(imported).isPresent();
        assertThat(imported.get().getSubject()).isEqualTo("Hello from SMTP");
        assertThat(imported.get().getSender().getEmail()).isEqualTo("sender@external.com");
        assertThat(imported.get().getBodyPlain()).contains("This is a test email body.");
        assertThat(imported.get().getThread()).isNotNull();
    }

    @Test
    void replyWithInReplyToHeader_threadResolvesCorrectly() throws Exception {
        // Import original
        GreenMailUtil.sendTextEmailTest(
                "inbox@example.com",
                "alice@external.com",
                "Original Message",
                "Original body");
        assertThat(greenMail.waitForIncomingEmail(5000, 1)).isTrue();
        Optional<Message> importedOriginal = emailImportService.importMessage(
                greenMail.getReceivedMessages()[0]);
        assertThat(importedOriginal).isPresent();
        String originalMsgId = importedOriginal.get().getMessageIdHeader();

        // Build a reply with In-Reply-To pointing at the original
        Session session = Session.getInstance(new Properties());
        MimeMessage reply = new MimeMessage(session);
        reply.setFrom(new InternetAddress("bob@external.com"));
        reply.setRecipient(jakarta.mail.Message.RecipientType.TO,
                new InternetAddress("inbox@example.com"));
        reply.setSubject("Re: Original Message");
        reply.setText("Reply body text");
        reply.setHeader("Message-ID", "<reply-greenmail@test.com>");
        reply.setHeader("In-Reply-To", originalMsgId);
        reply.setSentDate(new Date());

        Optional<Message> importedReply = emailImportService.importMessage(reply);

        assertThat(importedReply).isPresent();
        assertThat(importedReply.get().getInReplyTo()).isEqualTo(originalMsgId);
        assertThat(importedReply.get().getThread().getId())
                .isEqualTo(importedOriginal.get().getThread().getId());
    }

    @Test
    void multipleEmailsToMailbox_allImportedSuccessfully() throws Exception {
        for (int i = 1; i <= 3; i++) {
            GreenMailUtil.sendTextEmailTest(
                    "inbox@example.com",
                    "sender" + i + "@external.com",
                    "Message " + i,
                    "Body " + i);
        }
        assertThat(greenMail.waitForIncomingEmail(5000, 3)).isTrue();

        MimeMessage[] received = greenMail.getReceivedMessages();
        int imported = 0;
        for (MimeMessage msg : received) {
            if (emailImportService.importMessage(msg).isPresent()) {
                imported++;
            }
        }
        assertThat(imported).isEqualTo(3);
    }

    @Test
    void duplicateImport_isIdempotent_viaGreenMail() throws Exception {
        GreenMailUtil.sendTextEmailTest(
                "inbox@example.com",
                "dup@external.com",
                "Duplicate Test",
                "Should only import once");
        assertThat(greenMail.waitForIncomingEmail(5000, 1)).isTrue();

        MimeMessage msg = greenMail.getReceivedMessages()[0];
        Optional<Message> first = emailImportService.importMessage(msg);
        Optional<Message> second = emailImportService.importMessage(msg);

        assertThat(first).isPresent();
        assertThat(second).isEmpty();
    }
}
