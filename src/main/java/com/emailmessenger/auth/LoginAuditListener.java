package com.emailmessenger.auth;

import com.emailmessenger.domain.AuthEventType;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Stamps {@code users.last_login_at} and writes a
 * {@link AuthEventType#LOGIN_SUCCESS} audit row on every successful
 * authentication — covers form login, remember-me re-auth, and the
 * programmatic {@code request.login()} call inside
 * {@code AuthController#register}. Anonymous principals are ignored so
 * unauthenticated requests can't touch the column or the audit log.
 */
@Component
class LoginAuditListener {

    private final UserActivityService activity;
    private final AuthEventService authEvents;

    LoginAuditListener(UserActivityService activity, AuthEventService authEvents) {
        this.activity = activity;
        this.authEvents = authEvents;
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Authentication auth = event.getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return;
        }
        String name = auth.getName();
        if (name == null || name.isBlank() || "anonymousUser".equals(name)) {
            return;
        }
        activity.recordLogin(name);
        authEvents.recordForEmail(name, AuthEventType.LOGIN_SUCCESS,
                ClientIp.fromCurrentRequest());
    }
}
