package com.emailmessenger.repository;

import com.emailmessenger.domain.SavedSearch;
import com.emailmessenger.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedSearchRepository extends JpaRepository<SavedSearch, Long> {

    List<SavedSearch> findByOwnerOrderByCreatedAtAsc(User owner);

    Optional<SavedSearch> findByIdAndOwner(Long id, User owner);

    Optional<SavedSearch> findByOwnerAndName(User owner, String name);

    long countByOwner(User owner);
}
