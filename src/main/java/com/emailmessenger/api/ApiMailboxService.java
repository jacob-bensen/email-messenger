package com.emailmessenger.api;

import com.emailmessenger.billing.PlanLimitService;
import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.ImapConnectionException;
import com.emailmessenger.email.MailAccountService;
import com.emailmessenger.repository.UserRepository;
import com.emailmessenger.web.MailboxProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * docs/API.md §5.10–5.14. Delegates to the same {@link MailAccountService}
 * the website's {@code /mailboxes} pages use; this class only validates input
 * and maps to the wire shape.
 */
@Service
class ApiMailboxService {

    static final int MAX_HOST_LENGTH = 255;
    static final int MAX_USERNAME_LENGTH = 254;
    static final int DEFAULT_PORT = 993;

    private final UserRepository users;
    private final MailAccountService mailAccounts;
    private final PlanLimitService planLimits;

    ApiMailboxService(UserRepository users, MailAccountService mailAccounts, PlanLimitService planLimits) {
        this.users = users;
        this.mailAccounts = mailAccounts;
        this.planLimits = planLimits;
    }

    @Transactional(readOnly = true)
    ApiDtos.MailboxList list(ApiPrincipal principal) {
        User owner = owner(principal);
        Long defaultId = defaultMailboxId(owner);
        return new ApiDtos.MailboxList(
                mailAccounts.list(owner).stream().map(a -> ApiDtos.MailboxResponse.of(a, defaultId)).toList(),
                planLimits.canCreateMailbox(owner));
    }

    ApiDtos.ProviderList providers() {
        return new ApiDtos.ProviderList(
                Arrays.stream(MailboxProvider.values()).map(ApiDtos.ProviderResponse::of).toList());
    }

    /** {@link MailAccountService#connect} owns the transaction, the plan cap and the IMAP check. */
    ApiDtos.MailboxResponse connect(ApiPrincipal principal, ApiDtos.ConnectMailboxRequest request) {
        String host = trimmed(request.host());
        String username = trimmed(request.username());
        int port = request.port() == null ? DEFAULT_PORT : request.port();
        boolean ssl = request.ssl() == null || request.ssl();
        if (host == null || host.length() > MAX_HOST_LENGTH) {
            throw invalid("Enter the IMAP host (at most " + MAX_HOST_LENGTH + " characters).");
        }
        if (port < 1 || port > 65535) {
            throw invalid("Port must be between 1 and 65535.");
        }
        if (username == null || username.length() > MAX_USERNAME_LENGTH) {
            throw invalid("Enter the username (at most " + MAX_USERNAME_LENGTH + " characters).");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw invalid("Enter the password.");
        }
        User owner = owner(principal);
        try {
            MailAccount saved = mailAccounts.connect(owner, host, port, ssl, username, request.password());
            return ApiDtos.MailboxResponse.of(saved, defaultMailboxId(owner));
        } catch (ImapConnectionException e) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "mailbox_connect_failed",
                    "Could not connect to that mailbox: " + e.getMessage());
        }
    }

    ApiDtos.SyncResponse sync(ApiPrincipal principal, long id) {
        User owner = owner(principal);
        try {
            MailAccountService.SyncResult result = mailAccounts.syncNow(owner, id);
            return new ApiDtos.SyncResponse(result.imported(),
                    ApiDtos.MailboxResponse.of(result.account(), defaultMailboxId(owner)));
        } catch (NoSuchElementException e) {
            throw noSuchMailbox();
        }
    }

    void delete(ApiPrincipal principal, long id) {
        if (!mailAccounts.delete(owner(principal), id)) {
            throw noSuchMailbox();
        }
    }

    private User owner(ApiPrincipal principal) {
        return users.findById(principal.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized", "Sign in again."));
    }

    /** Read fresh: the user row is loaded per request, and the FK is cleared when that mailbox is removed. */
    private static Long defaultMailboxId(User owner) {
        // getId() on the lazy proxy doesn't need the row loaded.
        return owner.getDefaultMailAccount() == null ? null : owner.getDefaultMailAccount().getId();
    }

    private static String trimmed(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static ApiException noSuchMailbox() {
        return new ApiException(HttpStatus.NOT_FOUND, "not_found", "No such mailbox.");
    }

    private static ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "validation_failed", message);
    }
}
