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
 * the value is missing.
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
    public Result search(User owner, String query, Pageable pageable) {
        Plan plan = planLimits.currentPlan(owner);
        if (plan != Plan.FREE) {
            return new Result(threads.searchIncludingBody(owner, query, pageable), false);
        }
        Page<EmailThread> page = threads.search(owner, query, pageable);
        boolean nag = threads.hasBodyOnlyMatch(owner, query);
        return new Result(page, nag);
    }

    public record Result(Page<EmailThread> page, boolean showBodySearchUpgradeNag) {}
}
