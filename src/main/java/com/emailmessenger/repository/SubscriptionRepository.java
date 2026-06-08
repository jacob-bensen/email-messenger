package com.emailmessenger.repository;

import com.emailmessenger.domain.Subscription;
import com.emailmessenger.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
}
