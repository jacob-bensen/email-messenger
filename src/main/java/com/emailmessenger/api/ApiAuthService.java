package com.emailmessenger.api;

import com.emailmessenger.auth.LoginThrottleService;
import com.emailmessenger.auth.UserService;
import com.emailmessenger.billing.PlanLimitService;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;

/**
 * Password sign-in for the mobile client. Credentials go through the same
 * {@link AuthenticationManager} as the website's form login, so the existing
 * success/failure listeners stamp {@code last_login_at}, write the auth-event
 * audit rows, and feed the shared {@link LoginThrottleService} lockout.
 */
@Service
class ApiAuthService {

    private final AuthenticationConfiguration authenticationConfiguration;
    private final LoginThrottleService throttle;
    private final UserService users;
    private final UserRepository userRepository;
    private final PlanLimitService planLimits;
    private final ApiTokenService tokens;

    ApiAuthService(AuthenticationConfiguration authenticationConfiguration,
                   LoginThrottleService throttle,
                   UserService users,
                   UserRepository userRepository,
                   PlanLimitService planLimits,
                   ApiTokenService tokens) {
        this.authenticationConfiguration = authenticationConfiguration;
        this.throttle = throttle;
        this.users = users;
        this.userRepository = userRepository;
        this.planLimits = planLimits;
        this.tokens = tokens;
    }

    @Transactional
    ApiDtos.LoginResponse login(ApiDtos.LoginRequest request, String clientIp) {
        if (throttle.isLocked(request.email(), clientIp)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "too_many_attempts",
                    "Too many failed sign-in attempts. Try again later.");
        }
        try {
            authenticationManager().authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));
        } catch (AuthenticationException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "invalid_credentials",
                    "Incorrect email or password.");
        }
        User user = users.requireByEmail(request.email());
        ApiTokenService.IssuedToken issued = tokens.issue(user, request.deviceName());
        return new ApiDtos.LoginResponse(issued.rawToken(), "Bearer",
                issued.expiresAt().toInstant(ZoneOffset.UTC),
                ApiDtos.MeResponse.of(user, planLimits.currentPlan(user)));
    }

    @Transactional(readOnly = true)
    ApiDtos.MeResponse me(ApiPrincipal principal) {
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized", "Sign in again."));
        return ApiDtos.MeResponse.of(user, planLimits.currentPlan(user));
    }

    private AuthenticationManager authenticationManager() {
        try {
            return authenticationConfiguration.getAuthenticationManager();
        } catch (Exception e) {
            throw new IllegalStateException("AuthenticationManager unavailable", e);
        }
    }
}
