package com.emailmessenger.admin;

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
import java.util.List;

/**
 * Weekly operator digest mailing MRR / ARR / new-paying-this-week / churn
 * to every address in {@link AdminProperties#getEmails()}. Reuses
 * {@link RevenueMetricsService} for the steady-state snapshot and runs two
 * cheap status-and-updatedAt counts against {@link SubscriptionRepository}
 * for the seven-day deltas. The digest is the operator's "is this thing
 * making money?" answer delivered to where they already read mail, so
 * they don't have to remember to open {@code /admin/revenue}.
 */
@Service
public class AdminWeeklyDigestService {

    private static final Logger log = LoggerFactory.getLogger(AdminWeeklyDigestService.class);
    private static final DateTimeFormatter WEEK_END_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    static final Duration LOOKBACK = Duration.ofDays(7);

    private final AdminProperties adminProperties;
    private final RevenueMetricsService metricsService;
    private final SubscriptionRepository subscriptions;
    private final JavaMailSender mailSender;
    private final SiteProperties site;
    private final Clock clock;

    @Value("${spring.mail.username:noreply@mailaim.app}")
    private String fromAddress = "noreply@mailaim.app";

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
        String body = renderBody(metrics, newPaying, churn, now);
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
        return "MailIM weekly: " + metrics.mrrFormatted() + " MRR, "
                + newPaying + " new, " + churn + " churn";
    }

    private String renderBody(RevenueMetrics metrics, long newPaying, long churn, LocalDateTime now) {
        LocalDate weekEnd = now.atZone(ZoneOffset.UTC).toLocalDate();
        String base = site.getBaseUrl();
        StringBuilder sb = new StringBuilder();
        sb.append("MailIM operator digest — week ending ")
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
        sb.append('\n');
        sb.append("Full dashboard: ").append(base).append("/admin/revenue\n");
        return sb.toString();
    }
}
