package com.emailmessenger.service;

import com.emailmessenger.domain.WaitlistEntry;
import com.emailmessenger.repository.WaitlistEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The referral path is income-critical: a bug here either silently drops
 * referrals (= lost virality) or lets users farm credit for themselves
 * (= bogus queue positions and a credibility hit). Coverage focuses on
 * those failure modes.
 */
class WaitlistReferralServiceTest {

    private WaitlistEntryRepository repo;
    private WaitlistReferralService service;

    @BeforeEach
    void setUp() {
        repo = mock(WaitlistEntryRepository.class);
        service = new WaitlistReferralService(repo);
    }

    @Test
    void blankTokenIsNoOp() {
        service.creditReferrer("", "new@example.com");
        service.creditReferrer(null, "new@example.com");
        service.creditReferrer("   ", "new@example.com");

        verify(repo, never()).findByReferralToken(anyString());
        verify(repo, never()).save(any());
    }

    @Test
    void unknownTokenIsNoOp() {
        when(repo.findByReferralToken("does-not-exist")).thenReturn(Optional.empty());

        service.creditReferrer("does-not-exist", "new@example.com");

        verify(repo, never()).save(any());
    }

    @Test
    void selfReferralIsRejected() {
        WaitlistEntry self = entry("user@example.com", "tok-self", 0);
        when(repo.findByReferralToken("tok-self")).thenReturn(Optional.of(self));

        service.creditReferrer("tok-self", "USER@example.com");

        verify(repo, never()).save(any());
        assertThat(self.getReferralsCount()).isZero();
    }

    @Test
    void validReferralIncrementsAndPersists() {
        WaitlistEntry referrer = entry("alice@example.com", "tok-a", 2);
        when(repo.findByReferralToken("tok-a")).thenReturn(Optional.of(referrer));

        service.creditReferrer("tok-a", "bob@example.com");

        assertThat(referrer.getReferralsCount()).isEqualTo(3);
        verify(repo).save(referrer);
    }

    @Test
    void positionStartsAtRawWhenNoReferralsYet() {
        WaitlistEntry e = entry("a@b.com", "tok", 0);
        setId(e, 50L);
        when(repo.countByIdLessThan(50L)).thenReturn(49L);

        long pos = service.effectivePosition(e);

        assertThat(pos).isEqualTo(50L);
    }

    @Test
    void positionSubtractsReferralSkipPerCreditedReferral() {
        WaitlistEntry e = entry("a@b.com", "tok", 2);
        setId(e, 250L);
        when(repo.countByIdLessThan(250L)).thenReturn(249L);

        long pos = service.effectivePosition(e);

        // 250 - 2 * 100 = 50
        assertThat(pos).isEqualTo(50L);
    }

    @Test
    void positionCannotGoBelowOne() {
        WaitlistEntry e = entry("a@b.com", "tok", 100);
        setId(e, 5L);
        when(repo.countByIdLessThan(5L)).thenReturn(4L);

        long pos = service.effectivePosition(e);

        // 5 - 100 * 100 would be -9995; clamped to 1
        assertThat(pos).isEqualTo(1L);
    }

    private static WaitlistEntry entry(String email, String token, int referralsCount) {
        WaitlistEntry e = new WaitlistEntry(email);
        try {
            Field tokenField = WaitlistEntry.class.getDeclaredField("referralToken");
            tokenField.setAccessible(true);
            tokenField.set(e, token);
            Field countField = WaitlistEntry.class.getDeclaredField("referralsCount");
            countField.setAccessible(true);
            countField.set(e, referralsCount);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
        return e;
    }

    private static void setId(WaitlistEntry e, long id) {
        try {
            Field idField = WaitlistEntry.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(e, id);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }
}
