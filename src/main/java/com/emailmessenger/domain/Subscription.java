package com.emailmessenger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private Plan plan;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "stripe_customer_id", nullable = false, length = 255)
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id", length = 255)
    private String stripeSubscriptionId;

    @Column(name = "stripe_price_id", length = 255)
    private String stripePriceId;

    @Column(name = "trial_ends_at")
    private LocalDateTime trialEndsAt;

    @Column(name = "current_period_end")
    private LocalDateTime currentPeriodEnd;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Subscription() {}

    public Subscription(User user, String stripeCustomerId, String status) {
        this.user = user;
        this.stripeCustomerId = stripeCustomerId;
        this.status = status;
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
    public Plan getPlan() { return plan; }
    public String getStatus() { return status; }
    public String getStripeCustomerId() { return stripeCustomerId; }
    public String getStripeSubscriptionId() { return stripeSubscriptionId; }
    public String getStripePriceId() { return stripePriceId; }
    public LocalDateTime getTrialEndsAt() { return trialEndsAt; }
    public LocalDateTime getCurrentPeriodEnd() { return currentPeriodEnd; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setPlan(Plan plan) { this.plan = plan; }
    public void setStatus(String status) { this.status = status; }
    public void setStripeSubscriptionId(String id) { this.stripeSubscriptionId = id; }
    public void setStripePriceId(String id) { this.stripePriceId = id; }
    public void setTrialEndsAt(LocalDateTime t) { this.trialEndsAt = t; }
    public void setCurrentPeriodEnd(LocalDateTime t) { this.currentPeriodEnd = t; }
}
