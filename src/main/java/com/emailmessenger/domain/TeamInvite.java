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
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "team_invites")
public class TeamInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invited_by_user_id", nullable = false)
    private User invitedBy;

    @Column(name = "invitee_email", nullable = false, length = 254)
    private String inviteeEmail;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected TeamInvite() {}

    public TeamInvite(Team team, User invitedBy, String inviteeEmail,
                      String tokenHash, LocalDateTime expiresAt) {
        this.team = team;
        this.invitedBy = invitedBy;
        this.inviteeEmail = inviteeEmail;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() { return id; }
    public Team getTeam() { return team; }
    public User getInvitedBy() { return invitedBy; }
    public String getInviteeEmail() { return inviteeEmail; }
    public String getTokenHash() { return tokenHash; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setAcceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }

    public boolean isAccepted() { return acceptedAt != null; }
    public boolean isRevoked()  { return revokedAt != null; }
    public boolean isExpired(LocalDateTime now) { return !now.isBefore(expiresAt); }
    public boolean isPending(LocalDateTime now) {
        return !isAccepted() && !isRevoked() && !isExpired(now);
    }
}
