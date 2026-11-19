package com.emailmessenger.api;

import com.emailmessenger.billing.PlanLimitExceededException;
import com.emailmessenger.billing.PlanLimitService;
import com.emailmessenger.billing.PlanLimits;
import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.ImapConnectionException;
import com.emailmessenger.email.MailAccountService;
import com.emailmessenger.email.MailboxSyncService;
import com.emailmessenger.repository.UserRepository;
import com.emailmessenger.web.MailboxForm;
import com.emailmessenger.web.MailboxProvider;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

/**
 * docs/API.md §5.10–5.14. Delegates to the same {@link MailAccountService},
 * {@link MailboxSyncService} and {@link MailboxForm} rules as the website's
 * {@code /mailboxes}; this class only maps to and from the wire shape.
 */
@Service
class ApiMailboxService {

    /** Report the first violation in form order, so the message is stable. */
    private static final List<String> FIELD_ORDER = List.of("host", "port", "username", "password");

    private final UserRepository users;
    private final MailAccountService mailAccounts;
    private final MailboxSyncService syncService;
    private final PlanLimitService planLimits;
    private final Validator validator;

    ApiMailboxService(UserRepository users, MailAccountService mailAccounts, MailboxSyncService syncService,
                      PlanLimitService planLimits, Validator validator) {
        this.users = users;
        this.mailAccounts = mailAccounts;
        this.syncService = syncService;
        this.planLimits = planLimits;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    ApiDtos.MailboxList list(ApiPrincipal principal) {
        User owner = owner(principal);
        Long defaultId = defaultId(owner);
        long cap = planLimits.limitsFor(owner).mailboxes();
        return new ApiDtos.MailboxList(
                mailAccounts.list(owner).stream().map(a -> ApiDtos.MailboxResponse.of(a, defaultId)).toList(),
                cap == PlanLimits.UNLIMITED ? null : cap);
    }

    ApiDtos.ProviderList providers() {
        return new ApiDtos.ProviderList(
                Arrays.stream(MailboxProvider.values()).map(ApiDtos.ProviderResponse::of).toList());
    }

    /** Not transactional here: {@link MailAccountService#connect} owns the transaction. */
    ApiDtos.MailboxResponse connect(ApiPrincipal principal, ApiDtos.ConnectMailboxRequest request) {
        MailboxForm form = new MailboxForm();
        form.setHost(request.host());
        if (request.port() != null) form.setPort(request.port());
        if (request.ssl() != null) form.setSsl(request.ssl());
        form.setUsername(request.username());
        form.setPassword(request.password());
        validator.validate(form).stream()
                .min(Comparator.comparingInt(ApiMailboxService::fieldRank))
                .ifPresent(v -> {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "validation_failed", messageFor(v));
                });

        User owner = owner(principal);
        try {
            MailAccount saved = mailAccounts.connect(owner, form.getHost(), form.getPort(), form.isSsl(),
                    form.getUsername(), form.getPassword());
            return ApiDtos.MailboxResponse.of(saved, defaultId(owner));
        } catch (PlanLimitExceededException e) {
            throw new ApiException(HttpStatus.FORBIDDEN, "plan_limit_reached",
                    "Your " + planName(e) + " plan allows " + e.getLimit() + " mailbox"
                            + (e.getLimit() == 1 ? "" : "es") + ". Upgrade on the website to connect more.");
        } catch (ImapConnectionException e) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "mailbox_connect_failed",
                    "Could not connect to that mailbox: " + e.getMessage());
        }
    }

    ApiDtos.SyncResponse sync(ApiPrincipal principal, long id) {
        User owner = owner(principal);
        try {
            MailboxSyncService.SyncOutcome outcome = syncService.syncNow(owner, id);
            return new ApiDtos.SyncResponse(outcome.imported(),
                    ApiDtos.MailboxResponse.of(outcome.account(), defaultId(owner)));
        } catch (NoSuchElementException e) {
            throw noSuchMailbox();
        }
    }

    void remove(ApiPrincipal principal, long id) {
        if (!mailAccounts.delete(owner(principal), id)) {
            throw noSuchMailbox();
        }
    }

    private User owner(ApiPrincipal principal) {
        return users.findById(principal.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized", "Sign in again."));
    }

    /** getId() on the lazy proxy doesn't need the row loaded. */
    private static Long defaultId(User owner) {
        return owner.getDefaultMailAccount() == null ? null : owner.getDefaultMailAccount().getId();
    }

    private static int fieldRank(ConstraintViolation<?> violation) {
        int rank = FIELD_ORDER.indexOf(violation.getPropertyPath().toString());
        return rank < 0 ? FIELD_ORDER.size() : rank;
    }

    private static String messageFor(ConstraintViolation<?> violation) {
        String message = violation.getMessage();
        // Bare @Size messages ("size must be between …") don't say which field.
        return message.endsWith(".") ? message : violation.getPropertyPath() + ": " + message + ".";
    }

    private static String planName(PlanLimitExceededException e) {
        String name = e.getCurrentPlan().name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private static ApiException noSuchMailbox() {
        return new ApiException(HttpStatus.NOT_FOUND, "not_found", "No such mailbox.");
    }
}
