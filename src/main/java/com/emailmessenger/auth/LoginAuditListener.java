package com.emailmessenger.auth;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Stamps {@code users.last_login_at} on every successful authentication
 * — covers form login, remember-me re-auth, and the programmatic
 * {@code request.login()} call inside {@code AuthController#register}.
 * Anonymous principals are ignored so unauthenticated requests can't
 * touch the column.
 */
@Component
class LoginAuditListener {

    private final UserActivityService activity;

    LoginAuditListener(UserActivityService activity) {
        this.activity = activity;
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
    }
}
