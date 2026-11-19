package com.emailmessenger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** One {@code Idempotency-Key} of an API send (docs/API.md §5.8.1). */
@Entity
@Table(name = "api_send_requests")
public class ApiSendRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, updatable = false, length = 64)
    private String requestHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "conversation_key", length = 64)
    private String conversationKey;

    @Column(name = "thread_id")
    private Long threadId;

    @Column(name = "message_id")
    private Long messageId;

    protected ApiSendRequest() {}

    public ApiSendRequest(Long userId, String idempotencyKey, String requestHash, LocalDateTime createdAt) {
        this.userId = userId;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getRequestHash() { return requestHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public String getConversationKey() { return conversationKey; }
    public Long getThreadId() { return threadId; }
    public Long getMessageId() { return messageId; }

    public boolean isCompleted() { return completedAt != null; }

    public void complete(String conversationKey, long threadId, long messageId, LocalDateTime at) {
        this.conversationKey = conversationKey;
        this.threadId = threadId;
        this.messageId = messageId;
        this.completedAt = at;
    }
}
