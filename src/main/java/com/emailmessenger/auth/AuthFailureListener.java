package com.emailmessenger.auth;

import com.emailmessenger.domain.AuthEventType;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.stereotype.Component;

/**
 * Records {@link AuthEventType#LOGIN_FAILURE} for every failed form
 * login, then — if the failure pushes the email's running tally over
 * the throttle threshold — emits a matching
 * {@link AuthEventType#ACCOUNT_LOCKED} row so the next attempt's
 * lockout shows up in the user's "recent activity" panel rather than
 * appearing out of nowhere on a redirect to {@code /login?error=locked}.
 *
 * <p>The email and IP come off the current request rather than the
 * authentication token because the username parameter on
 * {@code UsernamePasswordAuthenticationToken} can be a non-string
 * principal for unusual auth flows; the form-login path always carries
 * a string principal but using the parameter is no less reliable and
 * keeps the listener mechanism-agnostic.
 */
@Component
class AuthFailureListener {

    private final AuthEventService authEvents;
    private final LoginThrottleService throttle;

    AuthFailureListener(AuthEventService authEvents, LoginThrottleService throttle) {
        this.authEvents = authEvents;
        this.throttle = throttle;
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        Object principal = event.getAuthentication() != null
                ? event.getAuthentication().getPrincipal() : null;
        String email = principal != null ? principal.toString() : null;
        if (email == null || email.isBlank() || "anonymousUser".equals(email)) {
            return;
        }
        String ip = ClientIp.fromCurrentRequest();
        authEvents.recordForEmail(email, AuthEventType.LOGIN_FAILURE, ip);
        if (throttle.isLocked(email, ip)) {
            authEvents.recordForEmail(email, AuthEventType.ACCOUNT_LOCKED, ip);
        }
    }
}
