package com.emailmessenger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "acquisition_source", length = 64)
    private String acquisitionSource;

    @Column(name = "google_subject", length = 255)
    private String googleSubject;

    @Column(name = "password_set", nullable = false)
    private boolean passwordSet = true;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_inbox_visit_at")
    private LocalDateTime lastInboxVisitAt;

    @Column(name = "last_reengagement_sent_at")
    private LocalDateTime lastReengagementSentAt;

    @Column(name = "last_activation_nudge_sent_at")
    private LocalDateTime lastActivationNudgeSentAt;

    @Column(name = "last_activation_followup_sent_at")
    private LocalDateTime lastActivationFollowupSentAt;

    @Column(name = "last_activation_lastchance_sent_at")
    private LocalDateTime lastActivationLastChanceSentAt;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    protected User() {}

    public User(String email, String passwordHash, String displayName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
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
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public String getAcquisitionSource() { return acquisitionSource; }
    public String getGoogleSubject() { return googleSubject; }
    public boolean isPasswordSet() { return passwordSet; }
    public boolean isGoogleOnly() { return googleSubject != null && !passwordSet; }
    public boolean isEnabled() { return enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public LocalDateTime getLastInboxVisitAt() { return lastInboxVisitAt; }
    public LocalDateTime getLastReengagementSentAt() { return lastReengagementSentAt; }
    public LocalDateTime getLastActivationNudgeSentAt() { return lastActivationNudgeSentAt; }
    public LocalDateTime getLastActivationFollowupSentAt() { return lastActivationFollowupSentAt; }
    public LocalDateTime getLastActivationLastChanceSentAt() { return lastActivationLastChanceSentAt; }
    public LocalDateTime getEmailVerifiedAt() { return emailVerifiedAt; }
    public boolean isEmailVerified() { return emailVerifiedAt != null; }

    public void setEmail(String email) { this.email = email; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setAcquisitionSource(String acquisitionSource) { this.acquisitionSource = acquisitionSource; }
    public void setGoogleSubject(String googleSubject) { this.googleSubject = googleSubject; }
    public void setPasswordSet(boolean passwordSet) { this.passwordSet = passwordSet; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public void setLastInboxVisitAt(LocalDateTime lastInboxVisitAt) { this.lastInboxVisitAt = lastInboxVisitAt; }
    public void setLastReengagementSentAt(LocalDateTime ts) { this.lastReengagementSentAt = ts; }
    public void setLastActivationNudgeSentAt(LocalDateTime ts) { this.lastActivationNudgeSentAt = ts; }
    public void setLastActivationFollowupSentAt(LocalDateTime ts) { this.lastActivationFollowupSentAt = ts; }
    public void setLastActivationLastChanceSentAt(LocalDateTime ts) { this.lastActivationLastChanceSentAt = ts; }
    public void setEmailVerifiedAt(LocalDateTime ts) { this.emailVerifiedAt = ts; }
}
