package com.emailmessenger.repository;

import com.emailmessenger.domain.WaitlistEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, Long> {

    boolean existsByEmail(String email);
}
