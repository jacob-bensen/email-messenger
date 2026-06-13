package com.emailmessenger.repository;

import com.emailmessenger.domain.SavedSearch;
import com.emailmessenger.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class SavedSearchRepositoryTest {

    @Autowired SavedSearchRepository repo;
    @Autowired UserRepository users;

    private User owner;
    private User other;

    @BeforeEach
    void setUp() {
        owner = users.save(new User("owner@example.com", "hash", "Owner"));
        other = users.save(new User("other@example.com", "hash", "Other"));
    }

    @Test
    void persistsAndReturnsByOwnerInCreatedOrder() {
        repo.save(new SavedSearch(owner, "From Ada", null, "ada@example.com", null, false, false));
        repo.save(new SavedSearch(owner, "Last 30d unread", null, null, "30d", true, false));

        List<SavedSearch> found = repo.findByOwnerOrderByCreatedAtAsc(owner);
        assertThat(found).hasSize(2);
        assertThat(found).extracting(SavedSearch::getName)
                .containsExactly("From Ada", "Last 30d unread");
    }

    @Test
    void scopedByOwner() {
        repo.save(new SavedSearch(owner, "Mine", null, null, "7d", false, false));
        repo.save(new SavedSearch(other, "Yours", null, null, "7d", false, false));

        assertThat(repo.findByOwnerOrderByCreatedAtAsc(owner))
                .extracting(SavedSearch::getName).containsExactly("Mine");
        assertThat(repo.countByOwner(owner)).isEqualTo(1);
        assertThat(repo.countByOwner(other)).isEqualTo(1);
    }

    @Test
    void findByIdAndOwnerRefusesCrossUserAccess() {
        SavedSearch mine = repo.save(new SavedSearch(owner, "Mine", "invoice", null, null, false, false));

        assertThat(repo.findByIdAndOwner(mine.getId(), owner)).isPresent();
        assertThat(repo.findByIdAndOwner(mine.getId(), other)).isEmpty();
    }

    @Test
    void findByOwnerAndNameMatchesExact() {
        repo.save(new SavedSearch(owner, "From Ada", null, "ada@example.com", null, false, false));

        assertThat(repo.findByOwnerAndName(owner, "From Ada")).isPresent();
        assertThat(repo.findByOwnerAndName(owner, "From ada")).isEmpty();
        assertThat(repo.findByOwnerAndName(other, "From Ada")).isEmpty();
    }

    @Test
    void uniqueConstraintBlocksDuplicateNamesPerOwner() {
        repo.save(new SavedSearch(owner, "Duplicate", null, "ada@example.com", null, false, false));

        assertThatThrownBy(() -> {
            repo.saveAndFlush(new SavedSearch(owner, "Duplicate", null, null, "30d", false, false));
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void sameNameIsAllowedForDifferentOwners() {
        repo.save(new SavedSearch(owner, "Inbox dump", null, null, null, true, false));
        repo.saveAndFlush(new SavedSearch(other, "Inbox dump", null, null, null, false, true));

        assertThat(repo.countByOwner(owner)).isEqualTo(1);
        assertThat(repo.countByOwner(other)).isEqualTo(1);
    }
}
