package com.emailmessenger.web;

import com.emailmessenger.billing.PlanLimitService;
import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.EmailThreadRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gates body-content search behind a paid plan. Free users still get
 * subject + participant matches; any thread that would have matched on
 * body alone flips on the upgrade nag so the upsell is shown right where
 * the value is missing. An optional sender filter narrows the result set
 * to threads with at least one message from that participant.
 */
@Service
public class ThreadSearchService {

    private final EmailThreadRepository threads;
    private final PlanLimitService planLimits;

    ThreadSearchService(EmailThreadRepository threads, PlanLimitService planLimits) {
        this.threads = threads;
        this.planLimits = planLimits;
    }

    @Transactional(readOnly = true)
    public Result search(User owner, String query, String senderEmail, Pageable pageable) {
        String normalizedSender = (senderEmail == null || senderEmail.isBlank()) ? null : senderEmail.trim();
        if (query == null || query.isBlank()) {
            if (normalizedSender == null) {
                throw new IllegalArgumentException("search requires either a query or a sender filter");
            }
            return new Result(threads.findByOwnerAndSender(owner, normalizedSender, pageable), false);
        }
        Plan plan = planLimits.currentPlan(owner);
        if (plan != Plan.FREE) {
            return new Result(threads.searchIncludingBody(owner, query, normalizedSender, pageable), false);
        }
        Page<EmailThread> page = threads.search(owner, query, normalizedSender, pageable);
        boolean nag = threads.hasBodyOnlyMatch(owner, query, normalizedSender);
        return new Result(page, nag);
    }

    public record Result(Page<EmailThread> page, boolean showBodySearchUpgradeNag) {}
}
