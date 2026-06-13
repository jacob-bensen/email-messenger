package com.emailmessenger.activation;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * The {@code users.created_at} column is {@code @Column(updatable = false)},
 * so JPA refuses to roll the clock back through the entity setter. Tests
 * for the activation sweep need a "yesterday" signup without literally
 * sleeping 24h, so this test-only component reaches through the
 * {@link EntityManager} for a native UPDATE. Picked up by the application
 * component scan because it lives under {@code com.emailmessenger.**}.
 */
@Component
public class ActivationTestSupport {

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public int backdateCreatedAt(Long userId, LocalDateTime backdated) {
        return em.createNativeQuery(
                        "UPDATE users SET created_at = :ts WHERE id = :id")
                .setParameter("ts", backdated)
                .setParameter("id", userId)
                .executeUpdate();
    }
}
