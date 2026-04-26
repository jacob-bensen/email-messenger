package com.emailmessenger.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "thread_id", nullable = false)
    private EmailThread thread;

    @Column(name = "message_id_header", unique = true, length = 998)
    private String messageIdHeader;

    @Column(name = "in_reply_to", length = 998)
    private String inReplyTo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private Participant sender;

    @Column(length = 998)
    private String subject;

    @Column(name = "body_plain", columnDefinition = "TEXT")
    private String bodyPlain;

    @Column(name = "body_html", columnDefinition = "TEXT")
    private String bodyHtml;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MessageRecipient> recipients = new ArrayList<>();

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Attachment> attachments = new ArrayList<>();

    protected Message() {}

    public Message(EmailThread thread, Participant sender, String subject,
                   String bodyPlain, String bodyHtml, LocalDateTime sentAt) {
        this.thread = thread;
        this.sender = sender;
        this.subject = subject;
        this.bodyPlain = bodyPlain;
        this.bodyHtml = bodyHtml;
        this.sentAt = sentAt;
    }

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public void addRecipient(Participant participant, RecipientType type) {
        recipients.add(new MessageRecipient(this, participant, type));
    }

    public void addAttachment(Attachment attachment) {
        attachments.add(attachment);
    }

    public Long getId() { return id; }
    public EmailThread getThread() { return thread; }
    public String getMessageIdHeader() { return messageIdHeader; }
    public String getInReplyTo() { return inReplyTo; }
    public Participant getSender() { return sender; }
    public String getSubject() { return subject; }
    public String getBodyPlain() { return bodyPlain; }
    public String getBodyHtml() { return bodyHtml; }
    public LocalDateTime getSentAt() { return sentAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<MessageRecipient> getRecipients() { return Collections.unmodifiableList(recipients); }
    public List<Attachment> getAttachments() { return Collections.unmodifiableList(attachments); }

    public void setMessageIdHeader(String messageIdHeader) { this.messageIdHeader = messageIdHeader; }
    public void setInReplyTo(String inReplyTo) { this.inReplyTo = inReplyTo; }
}
