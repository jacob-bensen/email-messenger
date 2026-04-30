package com.emailmessenger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "waitlist_entries")
public class WaitlistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "referral_token", unique = true, length = 36)
    private String referralToken = UUID.randomUUID().toString();

    @Column(name = "referrals_count", nullable = false)
    private int referralsCount = 0;

    protected WaitlistEntry() {}

    public WaitlistEntry(String email) {
        this.email = email;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public Instant getCreatedAt() { return createdAt; }
    public String getReferralToken() { return referralToken; }
    public int getReferralsCount() { return referralsCount; }

    public void incrementReferralsCount() {
        this.referralsCount++;
    }
}
