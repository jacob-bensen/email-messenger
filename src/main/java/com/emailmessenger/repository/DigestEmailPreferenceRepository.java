package com.emailmessenger.repository;

import com.emailmessenger.domain.DigestEmailPreference;
import com.emailmessenger.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DigestEmailPreferenceRepository extends JpaRepository<DigestEmailPreference, Long> {

    Optional<DigestEmailPreference> findByUser(User user);

    Optional<DigestEmailPreference> findByOptOutToken(String optOutToken);
}
