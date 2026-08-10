package com.emailmessenger.email;

import com.emailmessenger.billing.PlanLimitService;
import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.repository.MailAccountRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Transactional lookups for the IDLE reconciler, split out of
 * {@link ImapIdleService} so its worker threads call them through the Spring
 * proxy (lazy user/subscription associations need an open session).
 */
@Component
class ImapIdleSupport {

    private final MailAccountRepository accounts;
    private final PlanLimitService planLimitService;
    private final ImapCredentialsResolver credentialsResolver;

    ImapIdleSupport(MailAccountRepository accounts,
                    PlanLimitService planLimitService,
                    ImapCredentialsResolver credentialsResolver) {
        this.accounts = accounts;
        this.planLimitService = planLimitService;
        this.credentialsResolver = credentialsResolver;
    }

    /**
     * Accounts that should hold an IDLE connection right now: not
     * circuit-broken, owned by a paid-tier user. Free stays on the 15-minute
     * poll — instant delivery is part of what the paid tiers buy.
     */
    @Transactional(readOnly = true)
    Set<Long> eligibleAccountIds() {
        Set<Long> ids = new LinkedHashSet<>();
        for (MailAccount account : accounts.findByPollingSuspendedFalse()) {
            if (planLimitService.currentPlan(account.getUser()) != Plan.FREE) {
                ids.add(account.getId());
            }
        }
        return ids;
    }

    /**
     * Fresh credentials for one (re)connect. For XOAUTH2 this exchanges the
     * refresh token for a new access token, and persists a rotated refresh
     * token when Google hands one back. Throws {@link ImapConnectionException}
     * when the account is gone or its credentials no longer resolve.
     */
    @Transactional
    ImapCredentials credentialsFor(Long accountId) {
        MailAccount account = accounts.findById(accountId)
                .orElseThrow(() -> new ImapConnectionException("Mail account no longer exists", null));
        ImapCredentials credentials = credentialsResolver.resolve(account);
        accounts.save(account); // persists a rotated refresh token, if any
        return credentials;
    }
}
