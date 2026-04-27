package com.emailmessenger.repository;

import com.emailmessenger.domain.WaitlistEntry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class WaitlistEntryRepositoryTest {

    @Autowired
    WaitlistEntryRepository repo;

    @Test
    void savesEntryAndChecksExistence() {
        repo.save(new WaitlistEntry("user@example.com"));

        assertThat(repo.existsByEmail("user@example.com")).isTrue();
        assertThat(repo.existsByEmail("other@example.com")).isFalse();
    }

    @Test
    void duplicateEmailViolatesUniqueConstraint() {
        repo.save(new WaitlistEntry("dup@example.com"));
        repo.flush();

        assertThatThrownBy(() -> {
            repo.saveAndFlush(new WaitlistEntry("dup@example.com"));
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void createdAtIsPopulatedOnSave() {
        var entry = repo.save(new WaitlistEntry("ts@example.com"));
        assertThat(entry.getCreatedAt()).isNotNull();
    }
}
