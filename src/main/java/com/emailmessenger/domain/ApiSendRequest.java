package com.emailmessenger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** One {@code Idempotency-Key} a user sent with an API send (docs/API.md §5.8.1). */
@Entity
@Table(name = "api_send_requests")
public class ApiSendRequest {

    public enum Status { PENDING, DONE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private long userId;

    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 100)
    private String idempotencyKey;

    @Column(nullable = false, updatable = false, length = 64)
    private String fingerprint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    @Column(name = "conversation_key", length = 64)
    private String conversationKey;

    @Column(name = "thread_id")
    private Long threadId;

    @Column(name = "message_id")
    private Long messageId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ApiSendRequest() {}

    public ApiSendRequest(long userId, String idempotencyKey, String fingerprint, LocalDateTime createdAt) {
        this.userId = userId;
        this.idempotencyKey = idempotencyKey;
        this.fingerprint = fingerprint;
        this.createdAt = createdAt;
        this.status = Status.PENDING;
    }

    public Long getId() { return id; }
    public long getUserId() { return userId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getFingerprint() { return fingerprint; }
    public Status getStatus() { return status; }
    public String getConversationKey() { return conversationKey; }
    public Long getThreadId() { return threadId; }
    public Long getMessageId() { return messageId; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void complete(String conversationKey, long threadId, long messageId) {
        this.status = Status.DONE;
        this.conversationKey = conversationKey;
        this.threadId = threadId;
        this.messageId = messageId;
    }
}
