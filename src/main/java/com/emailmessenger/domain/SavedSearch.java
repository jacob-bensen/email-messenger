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
@Table(name = "saved_searches")
public class SavedSearch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "query_text", length = 200)
    private String query;

    @Column(name = "sender_email", length = 254)
    private String senderEmail;

    @Column(name = "since_preset", length = 10)
    private String sincePreset;

    @Column(name = "require_unread", nullable = false)
    private boolean requireUnread;

    @Column(name = "require_attachments", nullable = false)
    private boolean requireAttachments;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_viewed_at")
    private LocalDateTime lastViewedAt;

    protected SavedSearch() {}

    public SavedSearch(User owner, String name, String query, String senderEmail,
                       String sincePreset, boolean requireUnread, boolean requireAttachments) {
        this.owner = owner;
        this.name = name;
        this.query = query;
        this.senderEmail = senderEmail;
        this.sincePreset = sincePreset;
        this.requireUnread = requireUnread;
        this.requireAttachments = requireAttachments;
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
    public User getOwner() { return owner; }
    public String getName() { return name; }
    public String getQuery() { return query; }
    public String getSenderEmail() { return senderEmail; }
    public String getSincePreset() { return sincePreset; }
    public boolean isRequireUnread() { return requireUnread; }
    public boolean isRequireAttachments() { return requireAttachments; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getLastViewedAt() { return lastViewedAt; }
    public void setLastViewedAt(LocalDateTime lastViewedAt) { this.lastViewedAt = lastViewedAt; }
}
