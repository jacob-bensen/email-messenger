package com.emailmessenger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "mail_accounts")
public class MailAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String host;

    @Column(nullable = false)
    private int port;

    @Column(nullable = false)
    private boolean ssl;

    @Column(nullable = false, length = 254)
    private String username;

    @Column(name = "password_ciphertext", nullable = false, columnDefinition = "TEXT")
    private String passwordCiphertext;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Column(name = "last_sync_error", length = 500)
    private String lastSyncError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected MailAccount() {}

    public MailAccount(User user, String host, int port, boolean ssl,
                       String username, String passwordCiphertext) {
        this.user = user;
        this.host = host;
        this.port = port;
        this.ssl = ssl;
        this.username = username;
        this.passwordCiphertext = passwordCiphertext;
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

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public boolean isSsl() { return ssl; }
    public String getUsername() { return username; }
    public String getPasswordCiphertext() { return passwordCiphertext; }
    public LocalDateTime getLastSyncedAt() { return lastSyncedAt; }
    public String getLastSyncError() { return lastSyncError; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void markSynced() {
        this.lastSyncedAt = LocalDateTime.now();
        this.lastSyncError = null;
    }

    public void markSyncError(String message) {
        this.lastSyncError = message == null ? null
                : (message.length() > 500 ? message.substring(0, 500) : message);
    }
}
