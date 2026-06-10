package com.emailmessenger.repository;

import com.emailmessenger.domain.Team;
import com.emailmessenger.domain.TeamInvite;
import com.emailmessenger.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TeamInviteRepository extends JpaRepository<TeamInvite, Long> {

    Optional<TeamInvite> findByTokenHash(String tokenHash);

    // Used by the onboarding checklist to flip step 4 to done. We treat
    // any invite the user has sent — pending, accepted, or expired — as
    // "they took the action"; only revoked invites are excluded, since
    // a user who clicked invite then cancelled hasn't actually
    // completed the step.
    @Query("SELECT COUNT(i) FROM TeamInvite i " +
            "WHERE i.invitedBy = :inviter AND i.revokedAt IS NULL")
    long countNonRevokedByInviter(@Param("inviter") User inviter);

    /**
     * Onboarding-funnel slice: how many users in the supplied cohort have
     * sent at least one non-revoked invite. Mirrors the per-user predicate
     * the onboarding checklist already uses, so the funnel and the
     * in-product checkmark stay in agreement. Empty cohort returns 0.
     */
    @Query("SELECT COUNT(DISTINCT i.invitedBy.id) FROM TeamInvite i " +
            "WHERE i.invitedBy.id IN :userIds AND i.revokedAt IS NULL")
    long countDistinctInvitersIn(@Param("userIds") Collection<Long> userIds);

    List<TeamInvite> findByTeamOrderByCreatedAtDesc(Team team);

    // Pending invite for a given invitee email on a given team — used to
    // block duplicate invites and to surface "we already invited that
    // address" in the UI without churning new tokens. Sorted DESC so the
    // most recent token (if multiple) wins ties.
    @Query("SELECT i FROM TeamInvite i " +
            "WHERE i.team = :team AND LOWER(i.inviteeEmail) = LOWER(:email) " +
            "AND i.acceptedAt IS NULL AND i.revokedAt IS NULL " +
            "AND i.expiresAt > :now " +
            "ORDER BY i.createdAt DESC")
    List<TeamInvite> findPendingForTeamAndEmail(@Param("team") Team team,
                                                @Param("email") String email,
                                                @Param("now") LocalDateTime now);
}
