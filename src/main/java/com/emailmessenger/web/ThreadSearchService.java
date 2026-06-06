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
 * to threads with at least one message from that participant. Chip
 * filters (date range, unread, has-attachment) AND onto whichever search
 * path is selected so the result set narrows the same way on Free and
 * paid plans.
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
    public Result search(User owner, String query, String senderEmail,
                         ThreadFilters filters, Pageable pageable) {
        ThreadFilters f = filters == null ? ThreadFilters.NONE : filters;
        String normalizedSender = (senderEmail == null || senderEmail.isBlank()) ? null : senderEmail.trim();
        if (query == null || query.isBlank()) {
            if (normalizedSender == null) {
                if (!f.isActive()) {
                    throw new IllegalArgumentException(
                            "search requires a query, sender filter, or at least one active chip");
                }
                return new Result(threads.findByOwnerFiltered(owner,
                        f.since(), f.requireUnread(), f.requireAttachments(), pageable), false);
            }
            return new Result(threads.findByOwnerAndSender(owner, normalizedSender,
                    f.since(), f.requireUnread(), f.requireAttachments(), pageable), false);
        }
        Plan plan = planLimits.currentPlan(owner);
        if (plan != Plan.FREE) {
            return new Result(threads.searchIncludingBody(owner, query, normalizedSender,
                    f.since(), f.requireUnread(), f.requireAttachments(), pageable), false);
        }
        Page<EmailThread> page = threads.search(owner, query, normalizedSender,
                f.since(), f.requireUnread(), f.requireAttachments(), pageable);
        boolean nag = threads.hasBodyOnlyMatch(owner, query, normalizedSender,
                f.since(), f.requireUnread(), f.requireAttachments());
        return new Result(page, nag);
    }

    public record Result(Page<EmailThread> page, boolean showBodySearchUpgradeNag) {}
}
