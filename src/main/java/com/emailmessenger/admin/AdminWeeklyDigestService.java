package com.emailmessenger.admin;

import com.emailmessenger.billing.BillingPeriod;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.Subscription;
import com.emailmessenger.repository.SubscriptionRepository;
import com.emailmessenger.web.SiteProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/**
 * Weekly operator digest mailing MRR / ARR / new-paying-this-week / churn
 * to every address in {@link AdminProperties#getEmails()}. Reuses
 * {@link RevenueMetricsService} for the steady-state snapshot and runs two
 * cheap status-and-updatedAt counts against {@link SubscriptionRepository}
 * for the seven-day deltas. The digest is the operator's "is this thing
 * making money?" answer delivered to where they already read mail, so
 * they don't have to remember to open {@code /admin/revenue}.
 *
 * <p>EPIC-18 M4 adds two win-back lines: the un-emailed paid-cancel queue
 * count (rows waiting for a "Send win-back" click on {@code /admin/revenue})
 * and the count + MRR of subscriptions that got a win-back email this week
 * and have since flipped back to {@code active}. Pushes both the action and
 * the proof-of-return into the same email, so the loop closes without
 * needing a dashboard visit.
 */
@Service
public class AdminWeeklyDigestService {

    private static final Logger log = LoggerFactory.getLogger(AdminWeeklyDigestService.class);
    private static final DateTimeFormatter WEEK_END_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    static final Duration LOOKBACK = Duration.ofDays(7);
    private static final List<Plan> PAID_PLANS = List.of(Plan.PRO, Plan.BUSINESS);

    private final AdminProperties adminProperties;
    private final RevenueMetricsService metricsService;
    private final SubscriptionRepository subscriptions;
    private final JavaMailSender mailSender;
    private final SiteProperties site;
    private final Clock clock;

    @Value("${spring.mail.username:noreply@conexusmail.com}")
    private String fromAddress = "noreply@conexusmail.com";

    AdminWeeklyDigestService(AdminProperties adminProperties,
                             RevenueMetricsService metricsService,
                             SubscriptionRepository subscriptions,
                             JavaMailSender mailSender,
                             SiteProperties site,
                             Clock clock) {
        this.adminProperties = adminProperties;
        this.metricsService = metricsService;
        this.subscriptions = subscriptions;
        this.mailSender = mailSender;
        this.site = site;
        this.clock = clock;
    }

    /**
     * Send one digest email per allowlisted operator. Returns the count of
     * messages actually dispatched; a per-recipient {@link MailException}
     * is logged and skipped so one bad relay doesn't stall the whole
     * sweep.
     */
    public int sendDigest() {
        List<String> recipients = adminProperties.getEmails();
        if (recipients.isEmpty()) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime weekAgo = now.minus(LOOKBACK);
        RevenueMetrics metrics = metricsService.snapshot();
        long newPaying = subscriptions.countByStatusAndUpdatedAtAfter("active", weekAgo);
        long churn = subscriptions.countByStatusAndUpdatedAtAfter("canceled", weekAgo);
        List<Subscription> paidCanceledThisWeek = paidCanceledBetween(weekAgo, now);
        EnumMap<Plan, long[]> perPlanChurn = perPlanChurn(paidCanceledThisWeek);
        WinBackWeeklySummary winBack = winBackSummary(paidCanceledThisWeek, weekAgo);
        String body = renderBody(metrics, newPaying, churn, perPlanChurn, winBack, now);
        String subject = buildSubject(metrics, newPaying, churn);

        int sent = 0;
        for (String recipient : recipients) {
            try {
                mailSender.send(compose(recipient, subject, body));
                sent++;
            } catch (MailException e) {
                log.warn("Operator digest send failed for {}: {}", recipient, e.getMessage());
            }
        }
        return sent;
    }

    private MimeMessage compose(String recipient, String subject, String body) {
        MimeMessage mime = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(body, false);
        } catch (MessagingException e) {
            throw new MailPreparationException("Could not compose operator digest", e);
        }
        return mime;
    }

    private String buildSubject(RevenueMetrics metrics, long newPaying, long churn) {
        return "ConexusMail weekly: " + metrics.mrrFormatted() + " MRR, "
                + newPaying + " new, " + churn + " churn";
    }

    private String renderBody(RevenueMetrics metrics,
                              long newPaying,
                              long churn,
                              EnumMap<Plan, long[]> perPlanChurn,
                              WinBackWeeklySummary winBack,
                              LocalDateTime now) {
        LocalDate weekEnd = now.atZone(ZoneOffset.UTC).toLocalDate();
        String base = site.getBaseUrl();
        StringBuilder sb = new StringBuilder();
        sb.append("ConexusMail operator digest — week ending ")
          .append(WEEK_END_FORMAT.format(weekEnd)).append(" UTC\n\n");
        sb.append("MRR:                 ").append(metrics.mrrFormatted()).append('\n');
        sb.append("ARR:                 ").append(metrics.arrFormatted()).append('\n');
        sb.append("Active subscribers:  ").append(metrics.activeSubscribers()).append('\n');
        sb.append("Annual mix:          ").append(metrics.annualSharePercent()).append("% annual / ")
          .append(100 - metrics.annualSharePercent()).append("% monthly\n");
        sb.append("In trial:            ").append(metrics.trialingSubscribers())
          .append(" (").append(metrics.trialsEndingSoon()).append(" ending in 7 days)\n\n");
        sb.append("Last 7 days:\n");
        sb.append("  New paying customers: ").append(newPaying).append('\n');
        sb.append("  Churn (canceled):     ").append(churn).append('\n');
        for (Plan plan : PAID_PLANS) {
            long[] row = perPlanChurn.getOrDefault(plan, new long[2]);
            String label = planLabel(plan) + ":";
            sb.append(String.format("    %-16s%d canceled (-%s MRR)%n",
                    label, row[0], RevenueMetricsService.formatCents(row[1])));
        }
        sb.append("  Win-back queue:       ").append(winBack.unemailed())
          .append(" un-emailed paid cancels\n");
        sb.append("  Recovered:            ").append(winBack.recoveredCount())
          .append(" (+").append(RevenueMetricsService.formatCents(winBack.recoveredMrrCents()))
          .append(" MRR)\n");
        sb.append('\n');
        sb.append("Full dashboard: ").append(base).append("/admin/revenue\n");
        return sb.toString();
    }

    private List<Subscription> paidCanceledBetween(LocalDateTime from, LocalDateTime to) {
        List<Subscription> all = subscriptions.findCanceledBetween(from, to);
        List<Subscription> paid = new ArrayList<>(all.size());
        for (Subscription sub : all) {
            Plan plan = sub.getPlan();
            if (plan != null && plan != Plan.FREE) {
                paid.add(sub);
            }
        }
        return paid;
    }

    private EnumMap<Plan, long[]> perPlanChurn(List<Subscription> paidCanceledThisWeek) {
        EnumMap<Plan, long[]> perPlan = new EnumMap<>(Plan.class);
        for (Subscription sub : paidCanceledThisWeek) {
            Plan plan = sub.getPlan();
            BillingPeriod period = sub.getBillingPeriod() == null
                    ? BillingPeriod.MONTHLY
                    : sub.getBillingPeriod();
            long[] row = perPlan.computeIfAbsent(plan, p -> new long[2]);
            row[0]++;
            row[1] += PlanPricing.monthlyCents(plan, period);
        }
        return perPlan;
    }

    /**
     * Combines the two win-back lines the digest surfaces. The un-emailed
     * count is the subset of this week's paid cancels still eligible for a
     * "Send win-back" click (no send stamp yet), so it doubles as the
     * operator's inbox-side view of what waits on {@code /admin/revenue}.
     * Recovered is the paid cohort whose win-back stamp landed inside the
     * same 7-day window and whose subscription is now {@code active} again
     * — the tight signal that this week's outreach paid off. MRR is summed
     * from {@link PlanPricing#monthlyCents} against each recovered row.
     */
    private WinBackWeeklySummary winBackSummary(List<Subscription> paidCanceledThisWeek,
                                                LocalDateTime weekAgo) {
        long unemailed = 0;
        for (Subscription sub : paidCanceledThisWeek) {
            if (sub.getLastWinBackEmailSentAt() == null) {
                unemailed++;
            }
        }
        long recoveredCount = 0;
        long recoveredMrrCents = 0;
        for (Subscription sub : subscriptions.findRecoveredByWinBackSince(weekAgo)) {
            Plan plan = sub.getPlan();
            if (plan == null || plan == Plan.FREE) {
                continue;
            }
            BillingPeriod period = sub.getBillingPeriod() == null
                    ? BillingPeriod.MONTHLY
                    : sub.getBillingPeriod();
            recoveredCount++;
            recoveredMrrCents += PlanPricing.monthlyCents(plan, period);
        }
        return new WinBackWeeklySummary(unemailed, recoveredCount, recoveredMrrCents);
    }

    private record WinBackWeeklySummary(long unemailed, long recoveredCount, long recoveredMrrCents) {}

    private static String planLabel(Plan plan) {
        return switch (plan) {
            case PRO -> "Pro";
            case BUSINESS -> "Business";
            case FREE -> "Free";
        };
    }
}
