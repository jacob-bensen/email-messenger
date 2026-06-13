package com.emailmessenger.repository;

import com.emailmessenger.domain.Attachment;
import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Message;
import com.emailmessenger.domain.MessageRecipient;
import com.emailmessenger.domain.Participant;
import com.emailmessenger.domain.RecipientType;
import com.emailmessenger.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AttachmentRepositoryTest {

    @Autowired AttachmentRepository attachmentRepo;
    @Autowired MessageRepository messageRepo;
    @Autowired EmailThreadRepository threadRepo;
    @Autowired ParticipantRepository participantRepo;
    @Autowired UserRepository userRepo;

    private Message savedMessage;

    @BeforeEach
    void setUp() {
        var owner = userRepo.save(new User("attach-owner@example.com", "h", null));
        var sender = participantRepo.save(new Participant("attach-sender@example.com", "Sender"));
        var thread = threadRepo.save(new EmailThread(owner, "Attachment test thread", "<attach@example.com>"));
        savedMessage = messageRepo.save(
                new Message(thread, sender, "Has attachments", "body", null, LocalDateTime.now()));
    }

    @Test
    void savesAndFindsAttachmentByMessageId() {
        var attachment = new Attachment(savedMessage, "report.pdf", "application/pdf", 102400L, "s3://bucket/report.pdf");
        attachmentRepo.save(attachment);

        List<Attachment> found = attachmentRepo.findByMessageId(savedMessage.getId());
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getFilename()).isEqualTo("report.pdf");
        assertThat(found.get(0).getMimeType()).isEqualTo("application/pdf");
        assertThat(found.get(0).getSizeBytes()).isEqualTo(102400L);
        assertThat(found.get(0).getCreatedAt()).isNotNull();
    }

    @Test
    void multipleAttachmentsPerMessage() {
        attachmentRepo.save(new Attachment(savedMessage, "image.png", "image/png", 51200L, null));
        attachmentRepo.save(new Attachment(savedMessage, "data.csv", "text/csv", 2048L, null));

        assertThat(attachmentRepo.findByMessageId(savedMessage.getId())).hasSize(2);
    }

    @Test
    void messageRecipientsPersistedWithCorrectTypes() {
        var toRecipient = participantRepo.save(new Participant("to@example.com", "To Recipient"));
        var ccRecipient = participantRepo.save(new Participant("cc@example.com", "CC Recipient"));

        savedMessage.addRecipient(toRecipient, RecipientType.TO);
        savedMessage.addRecipient(ccRecipient, RecipientType.CC);
        messageRepo.save(savedMessage);
        messageRepo.flush();

        var refreshed = messageRepo.findById(savedMessage.getId()).orElseThrow();
        var recipients = refreshed.getRecipients();
        assertThat(recipients).hasSize(2);
        assertThat(recipients.stream().map(MessageRecipient::getRecipientType))
                .containsExactlyInAnyOrder(RecipientType.TO, RecipientType.CC);
    }
}
