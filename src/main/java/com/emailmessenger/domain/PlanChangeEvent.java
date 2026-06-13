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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "plan_change_events")
public class PlanChangeEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_plan", nullable = false, length = 32)
    private Plan fromPlan;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_plan", nullable = false, length = 32)
    private Plan toPlan;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    protected PlanChangeEvent() {}

    public PlanChangeEvent(Subscription subscription, User user, Plan fromPlan, Plan toPlan) {
        this.subscription = subscription;
        this.user = user;
        this.fromPlan = fromPlan;
        this.toPlan = toPlan;
    }

    @PrePersist
    void prePersist() {
        occurredAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() { return id; }
    public Subscription getSubscription() { return subscription; }
    public User getUser() { return user; }
    public Plan getFromPlan() { return fromPlan; }
    public Plan getToPlan() { return toPlan; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
