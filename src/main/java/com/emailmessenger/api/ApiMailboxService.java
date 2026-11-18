package com.emailmessenger.api;

import com.emailmessenger.billing.PlanLimitExceededException;
import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.ImapConnectionException;
import com.emailmessenger.email.MailAccountService;
import com.emailmessenger.email.MailboxPollingService;
import com.emailmessenger.repository.MailAccountRepository;
import com.emailmessenger.repository.UserRepository;
import com.emailmessenger.web.MailboxProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Locale;

/**
 * docs/API.md §5.10–5.14. Connect, sync and remove go through the same
 * {@link MailAccountService} and {@link MailboxPollingService} calls as the
 * website's {@code MailboxController}; this class validates input and maps
 * to the wire shape.
 */
@Service
class ApiMailboxService {

    static final int MAX_HOST_LENGTH = 255;
    static final int MAX_USERNAME_LENGTH = 254;
    static final int DEFAULT_PORT = 993;

    private final UserRepository users;
    private final MailAccountRepository accounts;
    private final MailAccountService mailAccounts;
    private final MailboxPollingService polling;

    ApiMailboxService(UserRepository users, MailAccountRepository accounts,
                      MailAccountService mailAccounts, MailboxPollingService polling) {
        this.users = users;
        this.accounts = accounts;
        this.mailAccounts = mailAccounts;
        this.polling = polling;
    }

    @Transactional(readOnly = true)
    ApiDtos.MailboxList list(ApiPrincipal principal) {
        User owner = owner(principal);
        Long defaultId = defaultAccountId(owner);
        return new ApiDtos.MailboxList(mailAccounts.list(owner).stream()
                .map(a -> ApiDtos.MailboxResponse.of(a, defaultId))
                .toList());
    }

    ApiDtos.ProviderList providers() {
        return new ApiDtos.ProviderList(Arrays.stream(MailboxProvider.values())
                .map(ApiDtos.ProviderResponse::of)
                .toList());
    }

    /** Transaction boundary is {@link MailAccountService#connect}, as on the website. */
    ApiDtos.MailboxResponse connect(ApiPrincipal principal, ApiDtos.ConnectMailboxRequest request) {
        if (request == null) {
            throw invalid("The request body is missing or malformed.");
        }
        String host = request.host() == null ? "" : request.host().trim();
        String username = request.username() == null ? "" : request.username().trim();
        int port = request.port() == null ? DEFAULT_PORT : request.port();
        boolean ssl = request.ssl() == null || request.ssl();
        if (host.isEmpty() || host.length() > MAX_HOST_LENGTH) {
            throw invalid("Enter the IMAP host (at most " + MAX_HOST_LENGTH + " characters).");
        }
        if (port < 1 || port > 65535) {
            throw invalid("Port must be between 1 and 65535.");
        }
        if (username.isEmpty() || username.length() > MAX_USERNAME_LENGTH) {
            throw invalid("Enter the username (at most " + MAX_USERNAME_LENGTH + " characters).");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw invalid("Enter the password.");
        }
        User owner = owner(principal);
        MailAccount saved;
        try {
            saved = mailAccounts.connect(owner, host, port, ssl, username, request.password());
        } catch (PlanLimitExceededException e) {
            throw new ApiException(HttpStatus.FORBIDDEN, "plan_limit_reached", planLimitMessage(e));
        } catch (ImapConnectionException e) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "mailbox_connect_failed",
                    "Could not connect to that mailbox: " + e.getMessage());
        }
        return ApiDtos.MailboxResponse.of(saved, defaultAccountId(owner));
    }

    /** Not transactional: {@link MailboxPollingService#pollOne} runs its own transaction. */
    ApiDtos.MailboxResponse sync(ApiPrincipal principal, long id) {
        User owner = owner(principal);
        MailAccount account = accounts.findByIdAndUser(id, owner).orElseThrow(ApiMailboxService::noSuchMailbox);
        polling.pollOne(account.getId());
        MailAccount reloaded = accounts.findById(account.getId()).orElseThrow(ApiMailboxService::noSuchMailbox);
        return ApiDtos.MailboxResponse.of(reloaded, defaultAccountId(owner));
    }

    void delete(ApiPrincipal principal, long id) {
        if (!mailAccounts.delete(owner(principal), id)) {
            throw noSuchMailbox();
        }
    }

    static String planLimitMessage(PlanLimitExceededException e) {
        String plan = e.getCurrentPlan().name().charAt(0)
                + e.getCurrentPlan().name().substring(1).toLowerCase(Locale.ROOT);
        long limit = e.getLimit();
        return "Your " + plan + " plan includes " + limit + (limit == 1 ? " mailbox" : " mailboxes")
                + ". Upgrade on the website to connect more.";
    }

    private User owner(ApiPrincipal principal) {
        return users.findById(principal.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized", "Sign in again."));
    }

    private static Long defaultAccountId(User owner) {
        // getId() on the lazy proxy doesn't load the row.
        return owner.getDefaultMailAccount() == null ? null : owner.getDefaultMailAccount().getId();
    }

    private static ApiException noSuchMailbox() {
        return new ApiException(HttpStatus.NOT_FOUND, "not_found", "No such mailbox.");
    }

    private static ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "validation_failed", message);
    }
}
