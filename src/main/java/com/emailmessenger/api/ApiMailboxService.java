package com.emailmessenger.api;

import com.emailmessenger.billing.PlanLimitExceededException;
import com.emailmessenger.billing.PlanLimitService;
import com.emailmessenger.billing.PlanLimits;
import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.ImapConnectionException;
import com.emailmessenger.email.MailAccountService;
import com.emailmessenger.email.MailboxPollingService;
import com.emailmessenger.repository.MailAccountRepository;
import com.emailmessenger.repository.UserRepository;
import com.emailmessenger.web.MailboxForm;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * docs/API.md §5.10–5.15. Delegates to the same {@link MailAccountService} and
 * {@link MailboxPollingService} as the website's {@code /mailboxes}, and validates
 * connect input with the website's {@link MailboxForm} constraints.
 */
@Service
class ApiMailboxService {

    /** Field order for picking which violation to report first. */
    private static final List<String> FIELD_ORDER = List.of("host", "port", "username", "password");

    private final UserRepository users;
    private final MailAccountService mailAccounts;
    private final MailAccountRepository mailAccountRepository;
    private final MailboxPollingService polling;
    private final PlanLimitService planLimits;
    private final Validator validator;

    ApiMailboxService(UserRepository users, MailAccountService mailAccounts,
                      MailAccountRepository mailAccountRepository, MailboxPollingService polling,
                      PlanLimitService planLimits, Validator validator) {
        this.users = users;
        this.mailAccounts = mailAccounts;
        this.mailAccountRepository = mailAccountRepository;
        this.polling = polling;
        this.planLimits = planLimits;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    ApiDtos.MailboxList list(ApiPrincipal principal) {
        User owner = owner(principal);
        long cap = PlanLimits.forPlan(planLimits.currentPlan(owner)).mailboxes();
        // getId() on the lazy proxy doesn't need the row loaded.
        Long defaultId = owner.getDefaultMailAccount() == null ? null : owner.getDefaultMailAccount().getId();
        return new ApiDtos.MailboxList(
                mailAccounts.list(owner).stream().map(ApiDtos.MailboxResponse::of).toList(),
                defaultId,
                cap == PlanLimits.UNLIMITED ? null : cap);
    }

    /** Transactional through {@link MailAccountService#connect}, like the website. */
    ApiDtos.MailboxResponse connect(ApiPrincipal principal, ApiDtos.ConnectMailboxRequest request) {
        MailboxForm form = new MailboxForm();
        form.setHost(request.host());
        form.setPort(request.port() == null ? 993 : request.port());
        form.setSsl(request.ssl() == null || request.ssl());
        form.setUsername(request.username());
        form.setPassword(request.password());
        validator.validate(form).stream()
                .min(Comparator.comparingInt(ApiMailboxService::fieldRank))
                .ifPresent(v -> {
                    throw invalid(v.getMessage());
                });
        User owner = owner(principal);
        try {
            return ApiDtos.MailboxResponse.of(mailAccounts.connect(owner, form.getHost(), form.getPort(),
                    form.isSsl(), form.getUsername(), form.getPassword()));
        } catch (PlanLimitExceededException e) {
            throw new ApiException(HttpStatus.FORBIDDEN, "plan_limit_reached",
                    "Your " + planName(e) + " plan includes " + e.getLimit()
                            + (e.getLimit() == 1 ? " mailbox" : " mailboxes")
                            + ". Upgrade on the website to connect more.");
        } catch (ImapConnectionException e) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "mailbox_connect_failed",
                    "Could not connect to that mailbox: " + e.getMessage());
        }
    }

    /** Not transactional: {@link MailboxPollingService#pollOne} runs its own transaction around IMAP. */
    ApiDtos.SyncResponse sync(ApiPrincipal principal, String rawId) {
        MailAccount account = owned(owner(principal), rawId);
        int imported = polling.pollOne(account.getId());
        MailAccount reloaded = mailAccountRepository.findById(account.getId()).orElseThrow(ApiMailboxService::noSuchMailbox);
        return new ApiDtos.SyncResponse(imported, ApiDtos.MailboxResponse.of(reloaded));
    }

    void delete(ApiPrincipal principal, String rawId) {
        User owner = owner(principal);
        if (!mailAccounts.delete(owner, parseId(rawId))) {
            throw noSuchMailbox();
        }
    }

    private MailAccount owned(User owner, String rawId) {
        return mailAccountRepository.findByIdAndUser(parseId(rawId), owner).orElseThrow(ApiMailboxService::noSuchMailbox);
    }

    /** A non-integer id names no mailbox, so it is a 404 rather than a 400. */
    private static Long parseId(String rawId) {
        try {
            return Long.valueOf(rawId);
        } catch (NumberFormatException e) {
            throw noSuchMailbox();
        }
    }

    private User owner(ApiPrincipal principal) {
        return users.findById(principal.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized", "Sign in again."));
    }

    private static int fieldRank(ConstraintViolation<MailboxForm> violation) {
        int rank = FIELD_ORDER.indexOf(violation.getPropertyPath().toString());
        return rank < 0 ? FIELD_ORDER.size() : rank;
    }

    private static String planName(PlanLimitExceededException e) {
        String name = e.getCurrentPlan().name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private static ApiException noSuchMailbox() {
        return new ApiException(HttpStatus.NOT_FOUND, "not_found", "No such mailbox.");
    }

    private static ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "validation_failed", message);
    }
}
