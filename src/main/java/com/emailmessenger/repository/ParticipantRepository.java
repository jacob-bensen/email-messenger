package com.emailmessenger.repository;

import com.emailmessenger.domain.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    Optional<Participant> findByEmail(String email);

    boolean existsByEmail(String email);
}
