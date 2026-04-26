package com.emailmessenger.email;

import com.emailmessenger.domain.Message;
import com.emailmessenger.domain.RecipientType;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.repository.MessageRepository;
import com.emailmessenger.repository.ParticipantRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class EmailImportServiceTest {

    @Autowired EmailImportService importService;
    @Autowired MessageRepository messageRepo;
    @Autowired EmailThreadRepository threadRepo;
    @Autowired ParticipantRepository participantRepo;

    @Test
    void importCreatesThreadAndMessage() throws Exception {
        MimeMessage mail = plainMessage("<root@test.com>", "Hello project", "alice@test.com", null, null,
                "Hey team, kicking off the project.");

        Optional<Message> result = importService.importMessage(mail);

        assertThat(result).isPresent();
        Message msg = result.get();
        assertThat(msg.getId()).isNotNull();
        assertThat(msg.getSubject()).isEqualTo("Hello project");
        assertThat(msg.getBodyPlain()).isEqualTo("Hey team, kicking off the project.");
        assertThat(msg.getThread()).isNotNull();
        assertThat(msg.getThread().getRootMessageId()).isEqualTo("<root@test.com>");
        assertThat(msg.getSender().getEmail()).isEqualTo("alice@test.com");
    }

    @Test
    void replyViaInReplyToJoinsExistingThread() throws Exception {
        importService.importMessage(
                plainMessage("<root@test.com>", "Project update", "alice@test.com", null, null, "First."));

        MimeMessage reply = plainMessage("<reply@test.com>", "Re: Project update", "bob@test.com",
                "<root@test.com>", null, "Got it.");
        Optional<Message> result = importService.importMessage(reply);

        assertThat(result).isPresent();
        Message replyMsg = result.get();
        Message rootMsg = messageRepo.findByMessageIdHeader("<root@test.com>").orElseThrow();
        assertThat(replyMsg.getThread().getId()).isEqualTo(rootMsg.getThread().getId());
        assertThat(replyMsg.getThread().getMessageCount()).isEqualTo(2);
    }

    @Test
    void replyViaReferencesJoinsExistingThread() throws Exception {
        importService.importMessage(
                plainMessage("<msg1@test.com>", "Topic", "alice@test.com", null, null, "Original."));

        MimeMessage reply = plainMessage("<msg2@test.com>", "Re: Topic", "carol@test.com",
                null, "<msg1@test.com>", "Following up.");
        Optional<Message> result = importService.importMessage(reply);

        assertThat(result).isPresent();
        Message rootMsg = messageRepo.findByMessageIdHeader("<msg1@test.com>").orElseThrow();
        assertThat(result.get().getThread().getId()).isEqualTo(rootMsg.getThread().getId());
    }

    @Test
    void duplicateMessageIdIsSkipped() throws Exception {
        MimeMessage mail = plainMessage("<dup@test.com>", "Dupe", "alice@test.com", null, null, "Body.");
        importService.importMessage(mail);
        Optional<Message> second = importService.importMessage(mail);

        assertThat(second).isEmpty();
        assertThat(messageRepo.findAll().stream()
                .filter(m -> "<dup@test.com>".equals(m.getMessageIdHeader())).count()).isEqualTo(1);
    }

    @Test
    void senderParticipantIsDeduplicated() throws Exception {
        importService.importMessage(
                plainMessage("<m1@test.com>", "First", "alice@test.com", null, null, "A."));
        importService.importMessage(
                plainMessage("<m2@test.com>", "Second", "alice@test.com", null, null, "B."));

        assertThat(participantRepo.findAll().stream()
                .filter(p -> "alice@test.com".equals(p.getEmail())).count()).isEqualTo(1);
    }

    @Test
    void toAndCcRecipientsAreCaptured() throws Exception {
        Session session = Session.getInstance(new Properties());
        MimeMessage mail = new MimeMessage(session);
        mail.setHeader("Message-ID", "<recipients@test.com>");
        mail.setSubject("Recipients test");
        mail.setFrom(new InternetAddress("sender@test.com"));
        mail.setRecipients(jakarta.mail.Message.RecipientType.TO,
                InternetAddress.parse("to1@test.com, to2@test.com"));
        mail.setRecipients(jakarta.mail.Message.RecipientType.CC,
                InternetAddress.parse("cc1@test.com"));
        mail.setText("Body.");
        mail.setSentDate(new Date());

        Optional<Message> result = importService.importMessage(mail);

        assertThat(result).isPresent();
        Message msg = result.get();
        long toCount = msg.getRecipients().stream()
                .filter(r -> RecipientType.TO == r.getRecipientType()).count();
        long ccCount = msg.getRecipients().stream()
                .filter(r -> RecipientType.CC == r.getRecipientType()).count();
        assertThat(toCount).isEqualTo(2);
        assertThat(ccCount).isEqualTo(1);
    }

    @Test
    void bccRecipientsAreCaptured() throws Exception {
        Session session = Session.getInstance(new Properties());
        MimeMessage mail = new MimeMessage(session);
        mail.setHeader("Message-ID", "<bcc@test.com>");
        mail.setSubject("BCC test");
        mail.setFrom(new InternetAddress("sender@test.com"));
        mail.setRecipients(jakarta.mail.Message.RecipientType.TO,
                InternetAddress.parse("to@test.com"));
        mail.setRecipients(jakarta.mail.Message.RecipientType.BCC,
                InternetAddress.parse("hidden@test.com"));
        mail.setText("Body.");
        mail.setSentDate(new Date());

        Optional<Message> result = importService.importMessage(mail);

        assertThat(result).isPresent();
        long bccCount = result.get().getRecipients().stream()
                .filter(r -> RecipientType.BCC == r.getRecipientType()).count();
        assertThat(bccCount).isEqualTo(1);
    }

    // ---- helper ----

    private MimeMessage plainMessage(String messageId, String subject, String from,
                                     String inReplyTo, String references, String body)
            throws Exception {
        Session session = Session.getInstance(new Properties());
        MimeMessage msg = new MimeMessage(session);
        if (messageId != null) msg.setHeader("Message-ID", messageId);
        msg.setSubject(subject);
        msg.setFrom(new InternetAddress(from));
        msg.setRecipients(jakarta.mail.Message.RecipientType.TO,
                InternetAddress.parse("recipient@test.com"));
        if (inReplyTo != null) msg.setHeader("In-Reply-To", inReplyTo);
        if (references != null) msg.setHeader("References", references);
        msg.setText(body);
        msg.setSentDate(new Date());
        return msg;
    }
}
