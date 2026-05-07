package com.emailmessenger.repository;

import com.emailmessenger.domain.WaitlistEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, Long> {

    boolean existsByEmail(String email);

    Optional<WaitlistEntry> findByEmail(String email);

    Optional<WaitlistEntry> findByReferralToken(String referralToken);

    long countByIdLessThan(long id);

    List<WaitlistEntry> findTop10ByReferralsCountGreaterThanOrderByReferralsCountDescIdAsc(int min);
}
