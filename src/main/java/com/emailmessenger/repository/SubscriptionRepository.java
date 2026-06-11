package com.emailmessenger.repository;

import com.emailmessenger.domain.Subscription;
import com.emailmessenger.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByUser(User user);

    Optional<Subscription> findByStripeCustomerId(String stripeCustomerId);

    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);

    /**
     * Eager-fetches every subscription with its owning user in a single
     * round-trip. Sized for the operator dashboard (low-cardinality, all
     * rows already in pages of recent activity); not a row-store scan
     * once the deployment crosses tens of thousands of paying customers.
     */
    @Query("SELECT s FROM Subscription s JOIN FETCH s.user ORDER BY s.updatedAt DESC")
    List<Subscription> findAllWithUserNewestFirst();

    /**
     * Candidate set for the "Reconcile from Stripe" backfill — every row
     * whose local cadence is still unknown. Once the backfill fills the
     * field, the row drops out so a second invocation is a no-op.
     */
    List<Subscription> findByBillingPeriodIsNull();

    /**
     * Week-over-week counter used by the operator digest. The status sweep
     * keys on {@code updatedAt} so a trial-to-paid conversion (createdAt
     * older than the window but status flipped this week) and a true
     * brand-new paid signup both register as "new paying customers".
     */
    long countByStatusAndUpdatedAtAfter(String status, LocalDateTime cutoff);

    /**
     * Funnel-anchor cohort for the operator dashboard: subscription rows
     * created inside the rolling window, with their owning user eager-joined
     * so the metrics service can group by {@code acquisition_source} without
     * an N+1 lazy fetch. Incomplete checkouts are excluded — they represent
     * Stripe-rejected payment attempts, not started trials.
     */
    @Query("""
            SELECT s FROM Subscription s JOIN FETCH s.user
            WHERE s.createdAt >= :cutoff
              AND s.status IN ('trialing','active','canceled')
            """)
    List<Subscription> findTrialCohortSince(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Trial-end conversion email cohort: trialing rows on a paid plan whose
     * {@code trial_ends_at} lands inside the cool-off window from "now",
     * never previously stamped, with the owning user eager-joined for the
     * opt-out / addressee lookup. Status filter on {@code trialing} keeps
     * already-converted ({@code active}) and canceled rows out — the
     * email is meant only for the conversion-leak cohort. ENTERPRISE
     * is excluded because that's a sales-led path, mirroring the
     * existing in-app {@link com.emailmessenger.billing.TrialConversionNudgeService}.
     */
    @Query("""
            SELECT s FROM Subscription s JOIN FETCH s.user
            WHERE s.status = 'trialing'
              AND s.plan IN (com.emailmessenger.domain.Plan.PERSONAL,
                             com.emailmessenger.domain.Plan.TEAM)
              AND s.trialEndsAt IS NOT NULL
              AND s.trialEndsAt <= :endingBy
              AND s.lastTrialEndEmailSentAt IS NULL
            """)
    List<Subscription> findTrialEndCandidates(@Param("endingBy") LocalDateTime endingBy);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Subscription s SET s.lastTrialEndEmailSentAt = :ts WHERE s.id = :id")
    int touchTrialEndEmailSent(@Param("id") Long id, @Param("ts") LocalDateTime ts);

    /**
     * Conversion-attribution slice for {@code /admin/revenue}: every
     * subscription touched by the trial-end email inside the rolling
     * window, with the owning user eager-joined so the metrics service
     * can compute "sent → active" without an N+1. Status is read straight
     * off the row, so a sub that's since flipped to {@code active} counts
     * as converted, and one that lapsed to {@code canceled} counts as a
     * non-conversion.
     */
    @Query("""
            SELECT s FROM Subscription s JOIN FETCH s.user
            WHERE s.lastTrialEndEmailSentAt IS NOT NULL
              AND s.lastTrialEndEmailSentAt >= :cutoff
            """)
    List<Subscription> findTrialEndEmailedSince(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Onboarding-funnel terminal step: how many users in the supplied
     * cohort currently have an active paid subscription. Status is
     * compared lower-case to match {@link RevenueMetricsService}'s
     * normalization, even though all writers go through the
     * {@code "active"} literal today. Empty cohort returns 0.
     */
    @Query("""
            SELECT COUNT(DISTINCT s.user.id) FROM Subscription s
            WHERE s.user.id IN :userIds
              AND LOWER(s.status) = 'active'
            """)
    long countActiveOwnersIn(@Param("userIds") Collection<Long> userIds);

    /**
     * Currently-active subscribers on the given plan. Used by the Team-plan
     * adoption card to anchor "this is what the install base looks like
     * right now" alongside the rolling-window conversion split.
     */
    @Query("""
            SELECT COUNT(s) FROM Subscription s
            WHERE s.plan = :plan
              AND LOWER(s.status) IN ('active','trialing')
            """)
    long countEntitledOn(@Param("plan") com.emailmessenger.domain.Plan plan);

    /**
     * Cancellation cohort for the operator churn card: rows whose status is
     * {@code canceled} with the cancel write landing inside the half-open
     * window {@code [from, to)}, with the owning user eager-fetched. The
     * window is keyed on {@code updatedAt} because Stripe's
     * {@code customer.subscription.deleted} webhook is the writer of the
     * canceled status and {@code updatedAt} is stamped by the {@code @PreUpdate}
     * the same transaction the status flips. A subscription created last
     * year and canceled this morning therefore shows up as this morning's
     * churn — which is what the operator wants — instead of vanishing into
     * a year-old anchor.
     */
    @Query("""
            SELECT s FROM Subscription s JOIN FETCH s.user
            WHERE LOWER(s.status) = 'canceled'
              AND s.updatedAt >= :from
              AND s.updatedAt < :to
            """)
    List<Subscription> findCanceledBetween(@Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to);
}
