package com.emailmessenger.repository;

import com.emailmessenger.domain.SavedSearch;
import com.emailmessenger.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SavedSearchRepository extends JpaRepository<SavedSearch, Long> {

    List<SavedSearch> findByOwnerOrderByCreatedAtAsc(User owner);

    Optional<SavedSearch> findByIdAndOwner(Long id, User owner);

    Optional<SavedSearch> findByOwnerAndName(User owner, String name);

    long countByOwner(User owner);

    @Query("SELECT DISTINCT s.owner FROM SavedSearch s")
    List<User> findDistinctOwners();

    /**
     * Onboarding-funnel slice: how many users in the supplied cohort have
     * saved at least one search. Distinct on the owner side so multiple
     * saved searches still count once. Empty cohort returns 0.
     */
    @Query("SELECT COUNT(DISTINCT s.owner.id) FROM SavedSearch s WHERE s.owner.id IN :userIds")
    long countDistinctOwnersIn(@Param("userIds") Collection<Long> userIds);
}
