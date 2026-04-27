package com.emailmessenger.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "email_threads")
public class EmailThread {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 998)
    private String subject;

    @Column(name = "root_message_id", unique = true, length = 998)
    private String rootMessageId;

    @Column(name = "message_count", nullable = false)
    private int messageCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "thread", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sentAt ASC")
    private List<Message> messages = new ArrayList<>();

    protected EmailThread() {}

    public EmailThread(String subject, String rootMessageId) {
        this.subject = subject;
        this.rootMessageId = rootMessageId;
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addMessage(Message message) {
        messages.add(message);
        messageCount = messages.size();
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getSubject() { return subject; }
    public String getRootMessageId() { return rootMessageId; }
    public int getMessageCount() { return messageCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public List<Message> getMessages() { return Collections.unmodifiableList(messages); }

    public void setMessageCount(int messageCount) { this.messageCount = messageCount; }
}
