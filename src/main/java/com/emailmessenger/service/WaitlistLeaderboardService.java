package com.emailmessenger.service;

import com.emailmessenger.domain.WaitlistEntry;
import com.emailmessenger.repository.WaitlistEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class WaitlistLeaderboardService {

    private static final int LEADERBOARD_SIZE = 10;

    private final WaitlistEntryRepository repo;

    WaitlistLeaderboardService(WaitlistEntryRepository repo) {
        this.repo = repo;
    }

    public record LeaderboardEntry(int rank, String anonymizedEmail, int referralsCount) {}

    @Transactional(readOnly = true)
    public List<LeaderboardEntry> top10() {
        List<WaitlistEntry> rows =
                repo.findTop10ByReferralsCountGreaterThanOrderByReferralsCountDescIdAsc(0);
        List<LeaderboardEntry> out = new ArrayList<>(rows.size());
        int rank = 1;
        for (WaitlistEntry e : rows) {
            out.add(new LeaderboardEntry(rank++, anonymize(e.getEmail()), e.getReferralsCount()));
            if (rank > LEADERBOARD_SIZE) break;
        }
        return out;
    }

    /**
     * Public scoreboard must not leak full addresses. Show only the first
     * letter of the local-part plus the domain — e.g. {@code alice@gmail.com}
     * becomes {@code a***@gmail.com}. Degenerate inputs (no '@', empty
     * local-part) fall back to {@code ***} or {@code ***@<domain>}.
     */
    static String anonymize(String email) {
        if (email == null || email.isBlank()) {
            return "***";
        }
        int at = email.indexOf('@');
        if (at < 0) {
            return "***";
        }
        String local = email.substring(0, at);
        String domain = email.substring(at + 1);
        if (local.isEmpty()) {
            return "***@" + domain;
        }
        return local.charAt(0) + "***@" + domain;
    }
}
