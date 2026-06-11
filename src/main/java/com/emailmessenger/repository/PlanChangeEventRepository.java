package com.emailmessenger.repository;

import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.PlanChangeEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface PlanChangeEventRepository extends JpaRepository<PlanChangeEvent, Long> {

    /**
     * Number of distinct users who transitioned <em>into</em> {@code toPlan}
     * from {@code fromPlan} on or after {@code since}. Distinct on user
     * because a user can churn out + back into the same plan inside the
     * window and we only count them once toward conversion.
     */
    @Query("""
            SELECT COUNT(DISTINCT e.user.id) FROM PlanChangeEvent e
            WHERE e.toPlan = :toPlan
              AND e.fromPlan = :fromPlan
              AND e.occurredAt >= :since
            """)
    long countDistinctUsersByTransitionSince(@Param("fromPlan") Plan fromPlan,
                                             @Param("toPlan") Plan toPlan,
                                             @Param("since") LocalDateTime since);
}
