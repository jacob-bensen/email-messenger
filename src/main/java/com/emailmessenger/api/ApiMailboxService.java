package com.emailmessenger.api;

import com.emailmessenger.billing.PlanLimitExceededException;
import com.emailmessenger.billing.PlanLimitService;
import com.emailmessenger.billing.PlanLimits;
import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.ImapConnectionException;
import com.emailmessenger.email.MailAccountService;
import com.emailmessenger.email.MailboxPollingService;
import com.emailmessenger.repository.UserRepository;
import com.emailmessenger.web.MailboxForm;
import com.emailmessenger.web.MailboxProvider;
import com.emailmessenger.web.MailboxView;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

/**
 * docs/API.md §5.10–5.14. Delegates to the same {@link MailAccountService},
 * {@link MailboxPollingService} and {@link PlanLimitService} as the website's
 * {@code /mailboxes}; this class only maps errors and the wire shape.
 *
 * <p>Not transactional: connect and sync talk to IMAP inside their own
 * transactions, and holding an outer one open across that I/O gains nothing.
 */
@Service
class ApiMailboxService {

    private final UserRepository users;
    private final MailAccountService mailAccounts;
    private final MailboxPollingService polling;
    private final PlanLimitService planLimits;

    ApiMailboxService(UserRepository users, MailAccountService mailAccounts,
                      MailboxPollingService polling, PlanLimitService planLimits) {
        this.users = users;
        this.mailAccounts = mailAccounts;
        this.polling = polling;
        this.planLimits = planLimits;
    }

    ApiDtos.MailboxList list(ApiPrincipal principal) {
        User owner = owner(principal);
        List<ApiDtos.MailboxResponse> items = mailAccounts.list(owner).stream()
                .map(account -> ApiDtos.MailboxResponse.of(account, defaultId(owner)))
                .toList();
        long limit = planLimits.limitsFor(owner).mailboxes();
        boolean unlimited = limit == PlanLimits.UNLIMITED;
        return new ApiDtos.MailboxList(items, unlimited ? null : limit, unlimited || items.size() < limit);
    }

    List<ApiDtos.ProviderResponse> providers() {
        return Arrays.stream(MailboxProvider.values()).map(ApiDtos.ProviderResponse::of).toList();
    }

    ApiDtos.MailboxResponse connect(ApiPrincipal principal, MailboxForm form) {
        User owner = owner(principal);
        try {
            MailAccount saved = mailAccounts.connect(owner, form.getHost(), form.getPort(), form.isSsl(),
                    form.getUsername(), form.getPassword());
            return ApiDtos.MailboxResponse.of(saved, defaultId(owner));
        } catch (PlanLimitExceededException e) {
            throw new ApiException(HttpStatus.FORBIDDEN, "plan_limit_reached",
                    "Your " + planName(e) + " plan connects up to " + e.getLimit()
                            + " mailboxes. Upgrade on the website or remove one to connect another.");
        } catch (ImapConnectionException e) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "mailbox_connect_failed",
                    "Could not connect to that mailbox: " + e.getMessage() + ". "
                            + MailboxView.errorHint(e.getMessage()));
        }
    }

    ApiDtos.SyncResponse sync(ApiPrincipal principal, long mailboxId) {
        User owner = owner(principal);
        try {
            MailboxPollingService.SyncNowResult result = polling.syncNow(owner, mailboxId);
            return new ApiDtos.SyncResponse(result.imported(),
                    ApiDtos.MailboxResponse.of(result.account(), defaultId(owner)));
        } catch (NoSuchElementException e) {
            throw noSuchMailbox();
        }
    }

    void delete(ApiPrincipal principal, long mailboxId) {
        if (!mailAccounts.delete(owner(principal), mailboxId)) {
            throw noSuchMailbox();
        }
    }

    private User owner(ApiPrincipal principal) {
        return users.findById(principal.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized", "Sign in again."));
    }

    /** getId() on the lazy proxy does not need the row or a session. */
    private static Long defaultId(User owner) {
        return owner.getDefaultMailAccount() == null ? null : owner.getDefaultMailAccount().getId();
    }

    private static String planName(PlanLimitExceededException e) {
        String name = e.getCurrentPlan().name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private static ApiException noSuchMailbox() {
        return new ApiException(HttpStatus.NOT_FOUND, "not_found", "No such mailbox.");
    }
}
