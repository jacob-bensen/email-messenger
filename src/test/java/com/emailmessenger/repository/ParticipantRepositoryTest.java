package com.emailmessenger.repository;

import com.emailmessenger.domain.Participant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ParticipantRepositoryTest {

    @Autowired
    ParticipantRepository repo;

    @Test
    void savesAndFindsParticipantByEmail() {
        var p = new Participant("alice@example.com", "Alice Smith");
        repo.save(p);

        Optional<Participant> found = repo.findByEmail("alice@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getDisplayName()).isEqualTo("Alice Smith");
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    void existsByEmailReturnsTrueWhenPresent() {
        repo.save(new Participant("bob@example.com", "Bob"));

        assertThat(repo.existsByEmail("bob@example.com")).isTrue();
        assertThat(repo.existsByEmail("nobody@example.com")).isFalse();
    }

    @Test
    void emailIsUnique() {
        repo.save(new Participant("carol@example.com", "Carol"));
        repo.flush();

        var duplicate = new Participant("carol@example.com", "Carol2");
        org.junit.jupiter.api.Assertions.assertThrows(
                Exception.class, () -> { repo.saveAndFlush(duplicate); });
    }
}
