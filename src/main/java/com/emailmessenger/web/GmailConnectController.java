package com.emailmessenger.web;

import com.emailmessenger.auth.GoogleOAuthProperties;
import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.GmailOAuthClient;
import com.emailmessenger.email.GmailOAuthException;
import com.emailmessenger.email.ImapConnectionException;
import com.emailmessenger.email.MailAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Principal;

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
    private static final String CALLBACK_PATH = "/mailboxes/gmail/callback";

    private final GmailOAuthClient oauthClient;
    private final MailAccountService mailAccountService;
    private final UserService userService;
    private final GoogleOAuthProperties googleOAuthProperties;
    private final StringKeyGenerator stateGenerator = new Base64StringKeyGenerator(32);

    GmailConnectController(GmailOAuthClient oauthClient,
                           MailAccountService mailAccountService,
                           UserService userService,
                           GoogleOAuthProperties googleOAuthProperties) {
        this.oauthClient = oauthClient;
        this.mailAccountService = mailAccountService;
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
        request.getSession(true).setAttribute(SESSION_STATE, state);
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
        if (session != null) {
            session.removeAttribute(SESSION_STATE);
        }

        if (error != null) {
            redirectAttributes.addFlashAttribute("connectError",
                    "Google authorization was cancelled. Your mailbox was not connected.");
            return "redirect:/mailboxes/new?provider=gmail";
        }
        if (code == null || state == null || expectedState == null || !statesMatch(expectedState, state)) {
            redirectAttributes.addFlashAttribute("connectError",
                    "Authorization could not be verified. Please try connecting again.");
            return "redirect:/mailboxes/new?provider=gmail";
        }

        try {
            GmailOAuthClient.TokenResult tokens = oauthClient.exchangeCode(code, callbackUri(request));
            if (tokens.refreshToken() == null || tokens.refreshToken().isBlank()) {
                // Google only returns a refresh token when it hasn't already
                // granted one for this client; prompt=consent should force it,
                // so a null here means the user must revoke and retry.
                redirectAttributes.addFlashAttribute("connectError",
                        "Google didn't return a refresh token. Remove ConexusMail under your Google "
                                + "account's third-party access, then connect again.");
                return "redirect:/mailboxes/new?provider=gmail";
            }
            String email = oauthClient.fetchEmail(tokens.accessToken());
            User owner = userService.requireByEmail(principal.getName());
            // PlanLimitExceededException intentionally propagates to the
            // GlobalExceptionHandler (upgrade modal), matching the IMAP path.
            MailAccount saved = mailAccountService.connectGmailOAuth(
                    owner, email, tokens.refreshToken(), tokens.accessToken());
            return saved.getLastSyncError() == null
                    ? "redirect:/threads"
                    : "redirect:/mailboxes";
        } catch (ImapConnectionException e) {
            redirectAttributes.addFlashAttribute("connectError",
                    "Connected to Google, but the IMAP login failed: " + e.getMessage());
            return "redirect:/mailboxes/new?provider=gmail";
        } catch (GmailOAuthException e) {
            log.warn("Gmail OAuth connect failed: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("connectError",
                    "Could not complete Google authorization. Please try again.");
            return "redirect:/mailboxes/new?provider=gmail";
        }
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
