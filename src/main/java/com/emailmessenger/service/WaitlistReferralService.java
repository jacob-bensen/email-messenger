package com.emailmessenger.service;

import com.emailmessenger.domain.WaitlistEntry;
import com.emailmessenger.repository.WaitlistEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Pre-launch viral loop: each successful signup arriving with a {@code ?ref=}
 * token credits the referring entry. Each referral skips REFERRAL_SKIP places
 * off the referrer's queue position.
 */
@Service
public class WaitlistReferralService {

    static final int REFERRAL_SKIP = 100;

    private final WaitlistEntryRepository repo;

    WaitlistReferralService(WaitlistEntryRepository repo) {
        this.repo = repo;
    }

    /**
     * Credits the referrer (if any) for a new signup. Self-referrals are
     * silently ignored — anyone with their own token can't farm credit.
     * Unknown / blank tokens are also silently ignored so the new signup
     * never fails because of an invalid query parameter.
     */
    @Transactional
    public void creditReferrer(String referralToken, String newSignupEmail) {
        if (referralToken == null || referralToken.isBlank()) {
            return;
        }
        Optional<WaitlistEntry> referrer = repo.findByReferralToken(referralToken);
        if (referrer.isEmpty()) {
            return;
        }
        if (referrer.get().getEmail().equalsIgnoreCase(newSignupEmail)) {
            return;
        }
        referrer.get().incrementReferralsCount();
        repo.save(referrer.get());
    }

    /**
     * Effective queue position for {@code entry}: how many entries joined
     * before them, minus {@link #REFERRAL_SKIP} per credited referral.
     * Capped at 1 (you can never skip past the front of the line).
     */
    @Transactional(readOnly = true)
    public long effectivePosition(WaitlistEntry entry) {
        long rawPosition = repo.countByIdLessThan(entry.getId()) + 1;
        long boosted = rawPosition - ((long) entry.getReferralsCount() * REFERRAL_SKIP);
        return Math.max(1, boosted);
    }
}
