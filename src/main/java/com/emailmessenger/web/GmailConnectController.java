package com.emailmessenger.web;

import com.emailmessenger.auth.GoogleOAuthProperties;
import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.GmailOAuthClient;
import com.emailmessenger.email.GmailOAuthException;
import com.emailmessenger.email.ImapConnectionException;
import com.emailmessenger.email.MailAccountService;
import com.emailmessenger.email.MailboxPollingService;
import com.emailmessenger.repository.MailAccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Principal;
import java.util.Locale;
import java.util.NoSuchElementException;

/**
 * The modern Gmail connect path: OAuth authorization-code flow that yields a
 * refresh token, so ConexusMail logs in over IMAP with XOAUTH2 instead of an app
 * password. Reuses the "Continue with Google" client credentials; only the
 * scope differs (see {@link GmailOAuthClient}).
 *
 * <p>Distinct from {@code oauth2Login()} (sign-in): that mints a session,
 * this stores a mailbox. We drive the flow by hand rather than through
 * Spring Security's client filters so the callback never collides with the
 * login redirect endpoint and so the refresh token is ours to persist.
 */
@Controller
@RequestMapping("/mailboxes/gmail")
class GmailConnectController {

    private static final Logger log = LoggerFactory.getLogger(GmailConnectController.class);
    private static final String SESSION_STATE = "conexusmail.gmail.oauth.state";
    // When set, the callback re-authorizes this existing mailbox instead of
    // connecting a new one.
    private static final String SESSION_RECONNECT_ID = "conexusmail.gmail.oauth.reconnectId";
    private static final String CALLBACK_PATH = "/mailboxes/gmail/callback";

    private final GmailOAuthClient oauthClient;
    private final MailAccountService mailAccountService;
    private final MailAccountRepository mailAccountRepository;
    private final MailboxPollingService pollingService;
    private final UserService userService;
    private final GoogleOAuthProperties googleOAuthProperties;
    private final StringKeyGenerator stateGenerator = new Base64StringKeyGenerator(32);

    GmailConnectController(GmailOAuthClient oauthClient,
                           MailAccountService mailAccountService,
                           MailAccountRepository mailAccountRepository,
                           MailboxPollingService pollingService,
                           UserService userService,
                           GoogleOAuthProperties googleOAuthProperties) {
        this.oauthClient = oauthClient;
        this.mailAccountService = mailAccountService;
        this.mailAccountRepository = mailAccountRepository;
        this.pollingService = pollingService;
        this.userService = userService;
        this.googleOAuthProperties = googleOAuthProperties;
    }

    @GetMapping("/connect")
    String connect(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        if (!googleOAuthProperties.isEnabled()) {
            redirectAttributes.addFlashAttribute("connectError",
                    "Google sign-in isn't configured on this server. Use an app password instead.");
            return "redirect:/mailboxes/new?provider=gmail";
        }
        String state = stateGenerator.generateKey();
        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_STATE, state);
        session.removeAttribute(SESSION_RECONNECT_ID);
        return "redirect:" + oauthClient.buildAuthorizationUrl(callbackUri(request), state);
    }

    /**
     * Re-authorizes an existing XOAUTH2 mailbox whose refresh token Google
     * revoked or expired (e.g. consent screens in Testing mode expire tokens
     * after 7 days). Same consent flow as connect; the callback updates the
     * stored token in place instead of creating a new account.
     */
    @GetMapping("/reconnect/{id}")
    String reconnect(@PathVariable Long id,
                     HttpServletRequest request,
                     Principal principal,
                     RedirectAttributes redirectAttributes) {
        if (!googleOAuthProperties.isEnabled()) {
            redirectAttributes.addFlashAttribute("syncError",
                    "Google sign-in isn't configured on this server.");
            return "redirect:/mailboxes";
        }
        User owner = userService.requireByEmail(principal.getName());
        // Foreign or unknown id 404s, same posture as sync/delete.
        MailAccount account = mailAccountRepository.findByIdAndUser(id, owner)
                .orElseThrow(NoSuchElementException::new);
        String state = stateGenerator.generateKey();
        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_STATE, state);
        session.setAttribute(SESSION_RECONNECT_ID, account.getId());
        return "redirect:" + oauthClient.buildAuthorizationUrl(callbackUri(request), state);
    }

    @GetMapping("/callback")
    String callback(@RequestParam(value = "code", required = false) String code,
                    @RequestParam(value = "state", required = false) String state,
                    @RequestParam(value = "error", required = false) String error,
                    HttpServletRequest request,
                    Principal principal,
                    RedirectAttributes redirectAttributes) {
        HttpSession session = request.getSession(false);
        String expectedState = session == null ? null : (String) session.getAttribute(SESSION_STATE);
        Long reconnectId = session == null ? null : (Long) session.getAttribute(SESSION_RECONNECT_ID);
        if (session != null) {
            session.removeAttribute(SESSION_STATE);
            session.removeAttribute(SESSION_RECONNECT_ID);
        }
        // Reconnect errors land on /mailboxes (where the broken row lives),
        // connect errors back on the wizard.
        boolean reconnecting = reconnectId != null;
        String errorFlash = reconnecting ? "syncError" : "connectError";
        String errorTarget = reconnecting ? "redirect:/mailboxes" : "redirect:/mailboxes/new?provider=gmail";

        if (error != null) {
            redirectAttributes.addFlashAttribute(errorFlash,
                    "Google authorization was cancelled. Your mailbox was not "
                            + (reconnecting ? "reconnected." : "connected."));
            return errorTarget;
        }
        if (code == null || state == null || expectedState == null || !statesMatch(expectedState, state)) {
            redirectAttributes.addFlashAttribute(errorFlash,
                    "Authorization could not be verified. Please try again.");
            return errorTarget;
        }

        try {
            GmailOAuthClient.TokenResult tokens = oauthClient.exchangeCode(code, callbackUri(request));
            if (tokens.refreshToken() == null || tokens.refreshToken().isBlank()) {
                // Google only returns a refresh token when it hasn't already
                // granted one for this client; prompt=consent should force it,
                // so a null here means the user must revoke and retry.
                redirectAttributes.addFlashAttribute(errorFlash,
                        "Google didn't return a refresh token. Remove ConexusMail under your Google "
                                + "account's third-party access, then try again.");
                return errorTarget;
            }
            String email = oauthClient.fetchEmail(tokens.accessToken());
            User owner = userService.requireByEmail(principal.getName());
            if (reconnecting) {
                return completeReconnect(owner, reconnectId, email, tokens, redirectAttributes);
            }
            // PlanLimitExceededException intentionally propagates to the
            // GlobalExceptionHandler (upgrade modal), matching the IMAP path.
            MailAccount saved = mailAccountService.connectGmailOAuth(
                    owner, email, tokens.refreshToken(), tokens.accessToken());
            return saved.getLastSyncError() == null
                    ? "redirect:/threads"
                    : "redirect:/mailboxes";
        } catch (ImapConnectionException e) {
            redirectAttributes.addFlashAttribute(errorFlash,
                    "Connected to Google, but the IMAP login failed: " + e.getMessage());
            return errorTarget;
        } catch (GmailOAuthException e) {
            log.warn("Gmail OAuth {} failed: {}", reconnecting ? "reconnect" : "connect", e.getMessage());
            redirectAttributes.addFlashAttribute(errorFlash,
                    "Could not complete Google authorization. Please try again.");
            return errorTarget;
        }
    }

    /**
     * Reconnect leg of the callback: the authorized address must be the
     * mailbox's own (re-authorizing with a different Google account would
     * silently swap whose mail the row syncs), then the stored token is
     * replaced in place and a poll runs so the user sees fresh state at once.
     */
    private String completeReconnect(User owner, Long accountId, String email,
                                     GmailOAuthClient.TokenResult tokens,
                                     RedirectAttributes redirectAttributes) {
        MailAccount account = mailAccountRepository.findByIdAndUser(accountId, owner)
                .orElseThrow(NoSuchElementException::new);
        if (!account.getUsername().trim().toLowerCase(Locale.ROOT)
                .equals(email.trim().toLowerCase(Locale.ROOT))) {
            redirectAttributes.addFlashAttribute("syncError",
                    "You signed in as " + email + ", but this mailbox is "
                            + account.getUsername() + ". Reconnect with the same Google account.");
            return "redirect:/mailboxes";
        }
        mailAccountService.reconnectGmailOAuth(owner, accountId,
                tokens.refreshToken(), tokens.accessToken());
        pollingService.pollOne(accountId);
        MailAccount reloaded = mailAccountRepository.findById(accountId)
                .orElseThrow(NoSuchElementException::new);
        if (reloaded.getLastSyncError() != null) {
            redirectAttributes.addFlashAttribute("syncError",
                    "Reconnected to Google, but syncing still failed: " + reloaded.getLastSyncError());
        } else {
            redirectAttributes.addFlashAttribute("syncMessage", "Mailbox reconnected and synced.");
        }
        return "redirect:/mailboxes";
    }

    private static String callbackUri(HttpServletRequest request) {
        // Built identically on both legs so it matches the value registered in
        // the Google Cloud console and the one sent at authorization time.
        return ServletUriComponentsBuilder.fromContextPath(request)
                .path(CALLBACK_PATH)
                .build()
                .toUriString();
    }

    private static boolean statesMatch(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}
