package com.emailmessenger.service;

import com.emailmessenger.domain.WaitlistEntry;
import com.emailmessenger.repository.WaitlistEntryRepository;
import com.emailmessenger.service.WaitlistLeaderboardService.LeaderboardEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WaitlistLeaderboardServiceTest {

    private WaitlistEntryRepository repo;
    private WaitlistLeaderboardService service;

    @BeforeEach
    void setUp() {
        repo = mock(WaitlistEntryRepository.class);
        service = new WaitlistLeaderboardService(repo);
    }

    @Test
    void anonymizeKeepsFirstLetterAndDomain() {
        assertThat(WaitlistLeaderboardService.anonymize("alice@gmail.com"))
                .isEqualTo("a***@gmail.com");
        assertThat(WaitlistLeaderboardService.anonymize("Z@example.org"))
                .isEqualTo("Z***@example.org");
    }

    @Test
    void anonymizeHandlesEmptyLocalPart() {
        assertThat(WaitlistLeaderboardService.anonymize("@example.com"))
                .isEqualTo("***@example.com");
    }

    @Test
    void anonymizeHandlesNullOrBlank() {
        assertThat(WaitlistLeaderboardService.anonymize(null)).isEqualTo("***");
        assertThat(WaitlistLeaderboardService.anonymize("")).isEqualTo("***");
        assertThat(WaitlistLeaderboardService.anonymize("   ")).isEqualTo("***");
    }

    @Test
    void anonymizeHandlesAddressWithoutAtSign() {
        // Bean-validation should prevent this in production, but the formatter
        // must still degrade gracefully.
        assertThat(WaitlistLeaderboardService.anonymize("not-an-email")).isEqualTo("***");
    }

    @Test
    void top10ReturnsEmptyListWhenNoReferralsExist() {
        when(repo.findTop10ByReferralsCountGreaterThanOrderByReferralsCountDescIdAsc(eq(0)))
                .thenReturn(List.of());

        assertThat(service.top10()).isEmpty();
    }

    @Test
    void top10MapsRepoRowsToRankedAnonymizedEntries() {
        WaitlistEntry first = entry("ada@example.com", 5);
        WaitlistEntry second = entry("bob@gmail.com", 3);
        WaitlistEntry third = entry("c@x.io", 1);
        when(repo.findTop10ByReferralsCountGreaterThanOrderByReferralsCountDescIdAsc(eq(0)))
                .thenReturn(List.of(first, second, third));

        List<LeaderboardEntry> top = service.top10();

        assertThat(top).hasSize(3);
        assertThat(top.get(0)).isEqualTo(new LeaderboardEntry(1, "a***@example.com", 5));
        assertThat(top.get(1)).isEqualTo(new LeaderboardEntry(2, "b***@gmail.com", 3));
        assertThat(top.get(2)).isEqualTo(new LeaderboardEntry(3, "c***@x.io", 1));
    }

    @Test
    void top10NeverEmitsRawEmailAddresses() {
        WaitlistEntry sensitive = entry("ceo@huge-corp.example", 9);
        when(repo.findTop10ByReferralsCountGreaterThanOrderByReferralsCountDescIdAsc(eq(0)))
                .thenReturn(List.of(sensitive));

        LeaderboardEntry only = service.top10().get(0);

        assertThat(only.anonymizedEmail()).doesNotContain("ceo");
        assertThat(only.anonymizedEmail()).startsWith("c***@");
        assertThat(only.anonymizedEmail()).endsWith("@huge-corp.example");
    }

    private static WaitlistEntry entry(String email, int referralsCount) {
        WaitlistEntry e = new WaitlistEntry(email);
        try {
            Field rc = WaitlistEntry.class.getDeclaredField("referralsCount");
            rc.setAccessible(true);
            rc.set(e, referralsCount);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
        return e;
    }
}
