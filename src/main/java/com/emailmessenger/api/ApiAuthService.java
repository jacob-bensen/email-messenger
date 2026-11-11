package com.emailmessenger.api;

import com.emailmessenger.auth.ApiTokenService;
import com.emailmessenger.auth.LoginThrottleService;
import com.emailmessenger.auth.UserService;
import com.emailmessenger.billing.PlanLimitService;
import com.emailmessenger.domain.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;

/**
 * Password login for the mobile client. Credentials go through the same
 * {@link AuthenticationManager}, throttle, and audit trail as the browser's
 * form login; the only API-specific part is that success mints a bearer token
 * instead of a session.
 */
@Service
class ApiAuthService {

    private final AuthenticationManager authenticationManager;
    private final LoginThrottleService throttle;
    private final ApiTokenService tokens;
    private final UserService users;
    private final PlanLimitService planLimits;

    ApiAuthService(AuthenticationManager authenticationManager,
                   LoginThrottleService throttle,
                   ApiTokenService tokens,
                   UserService users,
                   PlanLimitService planLimits) {
        this.authenticationManager = authenticationManager;
        this.throttle = throttle;
        this.tokens = tokens;
        this.users = users;
        this.planLimits = planLimits;
    }

    LoginResponse login(LoginRequest request, String clientIp) {
        // Checked before the credentials so a locked account can't be probed
        // for "is the password right now" — mirrors LoginThrottleFilter.
        if (throttle.isLocked(request.email(), clientIp)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "too_many_attempts",
                    "Too many sign-in attempts. Try again in a few minutes.");
        }
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.email().trim(), request.password()));
        } catch (AuthenticationException e) {
            // One answer for unknown email, wrong password, and disabled account.
            throw new ApiException(HttpStatus.UNAUTHORIZED, "invalid_credentials",
                    "That email and password don't match.");
        }
        User user = users.requireByEmail(authentication.getName());
        ApiTokenService.IssuedToken issued = tokens.issue(user, request.deviceName());
        return new LoginResponse(issued.token(), issued.expiresAt().toInstant(ZoneOffset.UTC), me(user));
    }

    UserDto me(User user) {
        return UserDto.from(user, planLimits.currentPlan(user));
    }
}
