package com.emailmessenger.admin;

import com.emailmessenger.admin.FunnelMetrics.SourceFunnel;
import com.emailmessenger.domain.Subscription;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.SubscriptionRepository;
import com.emailmessenger.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rolling 30-day signup → trial → paid funnel for {@code /admin/revenue}.
 * The two cohort metrics (trial starts, paid conversions) are anchored on
 * the subscription's {@code created_at} rather than {@code updated_at}, so
 * a year-old active subscription whose row gets touched by a routine
 * webhook doesn't accidentally inflate this month's paid-conversion count.
 */
@Service
public class FunnelMetricsService {

    static final int WINDOW_DAYS = 30;
    private static final String DIRECT_LABEL = "Direct / unknown";
    private static final String STATUS_ACTIVE = "active";

    private final UserRepository users;
    private final SubscriptionRepository subscriptions;
    private final Clock clock;

    FunnelMetricsService(UserRepository users,
                         SubscriptionRepository subscriptions,
                         Clock clock) {
        this.users = users;
        this.subscriptions = subscriptions;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public FunnelMetrics snapshot() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minusDays(WINDOW_DAYS);

        List<User> recentSignups = users.findCreatedAtAfter(cutoff);
        List<Subscription> recentCohort = subscriptions.findTrialCohortSince(cutoff);

        Map<String, int[]> bySource = new LinkedHashMap<>();
        for (User u : recentSignups) {
            bucket(bySource, sourceOf(u))[0]++;
        }
        int paidConversions = 0;
        for (Subscription sub : recentCohort) {
            int[] row = bucket(bySource, sourceOf(sub.getUser()));
            row[1]++;
            if (STATUS_ACTIVE.equalsIgnoreCase(sub.getStatus())) {
                row[2]++;
                paidConversions++;
            }
        }

        int signups = recentSignups.size();
        int trialStarts = recentCohort.size();
        int trialRate = percentOf(trialStarts, signups);
        int paidRate = percentOf(paidConversions, trialStarts);

        List<SourceFunnel> sourceRows = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : bySource.entrySet()) {
            int[] v = entry.getValue();
            sourceRows.add(new SourceFunnel(
                    entry.getKey(), v[0], v[1], v[2],
                    percentOf(v[1], v[0]),
                    percentOf(v[2], v[1])));
        }
        sourceRows.sort(Comparator
                .comparingInt(SourceFunnel::signups).reversed()
                .thenComparing(SourceFunnel::sourceLabel));

        return new FunnelMetrics(WINDOW_DAYS, signups, trialStarts, paidConversions,
                trialRate, paidRate, sourceRows);
    }

    private static int[] bucket(Map<String, int[]> map, String key) {
        return map.computeIfAbsent(key, k -> new int[3]);
    }

    private static String sourceOf(User user) {
        if (user == null) {
            return DIRECT_LABEL;
        }
        String raw = user.getAcquisitionSource();
        if (raw == null || raw.isBlank()) {
            return DIRECT_LABEL;
        }
        return raw;
    }

    static int percentOf(int numerator, int denominator) {
        if (denominator <= 0) {
            return 0;
        }
        return (int) Math.round(100.0 * numerator / denominator);
    }
}
