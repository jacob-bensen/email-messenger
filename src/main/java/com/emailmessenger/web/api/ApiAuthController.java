package com.emailmessenger.web.api;

import com.emailmessenger.auth.AuthEventService;
import com.emailmessenger.auth.LoginThrottleService;
import com.emailmessenger.auth.UserService;
import com.emailmessenger.billing.PlanLimitService;
import com.emailmessenger.domain.AuthEventType;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * REST authentication for the mobile client. Contract lives in
 * {@code docs/API.md} §Auth — do not change either side of this without
 * updating that doc first.
 */
@RestController
@RequestMapping("/api/v1/auth")
class ApiAuthController {

    private final UserService users;
    private final PasswordEncoder passwordEncoder;
    private final ApiTokenService apiTokens;
    private final LoginThrottleService throttle;
    private final AuthEventService authEvents;
    private final PlanLimitService planLimits;

    ApiAuthController(UserService users,
                      PasswordEncoder passwordEncoder,
                      ApiTokenService apiTokens,
                      LoginThrottleService throttle,
                      AuthEventService authEvents,
                      PlanLimitService planLimits) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.apiTokens = apiTokens;
        this.throttle = throttle;
        this.authEvents = authEvents;
        this.planLimits = planLimits;
    }

    public record LoginRequest(@Email @NotBlank String email,
                               @NotBlank String password) {}

    public record UserPayload(String id, String email, String displayName,
                              String plan, boolean emailVerified) {

        static UserPayload from(User user, Plan plan) {
            return new UserPayload(
                    String.valueOf(user.getId()),
                    user.getEmail(),
                    user.getDisplayName(),
                    plan.name(),
                    user.isEmailVerified());
        }
    }

    public record LoginResponse(String token, UserPayload user) {}

    @PostMapping("/login")
    ResponseEntity<?> login(@RequestBody LoginRequest body, HttpServletRequest request) {
        if (body == null || body.email() == null || body.email().isBlank()
                || body.password() == null || body.password().isEmpty()) {
            return jsonError(HttpStatus.BAD_REQUEST, "bad_request");
        }
        String ip = clientIp(request);
        if (throttle.isLocked(body.email(), ip)) {
            authEvents.recordForEmail(body.email(), AuthEventType.ACCOUNT_LOCKED, ip);
            return jsonError(HttpStatus.UNAUTHORIZED, "unauthorized");
        }
        Optional<User> match = users.findByEmail(body.email());
        if (match.isEmpty()
                || !match.get().isEnabled()
                || !passwordEncoder.matches(body.password(), match.get().getPasswordHash())) {
            authEvents.recordForEmail(body.email(), AuthEventType.LOGIN_FAILURE, ip);
            return jsonError(HttpStatus.UNAUTHORIZED, "unauthorized");
        }
        User user = match.get();
        var issued = apiTokens.issue(user, request.getHeader("User-Agent"));
        authEvents.record(user, user.getEmail(), AuthEventType.LOGIN_SUCCESS, ip);
        Plan plan = planLimits.currentPlan(user);
        return ResponseEntity.ok(new LoginResponse(
                issued.plaintext(), UserPayload.from(user, plan)));
    }

    @GetMapping("/me")
    ResponseEntity<?> me(Authentication authentication) {
        ApiPrincipal principal = requirePrincipal(authentication);
        if (principal == null) {
            return jsonError(HttpStatus.UNAUTHORIZED, "unauthorized");
        }
        User user = principal.token().getUser();
        Plan plan = planLimits.currentPlan(user);
        return ResponseEntity.ok(UserPayload.from(user, plan));
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(Authentication authentication) {
        ApiPrincipal principal = requirePrincipal(authentication);
        if (principal != null && authentication.getCredentials() instanceof String plaintext) {
            apiTokens.revoke(plaintext);
        }
        return ResponseEntity.noContent().build();
    }

    private static ApiPrincipal requirePrincipal(Authentication authentication) {
        return (authentication != null && authentication.getPrincipal() instanceof ApiPrincipal p)
                ? p : null;
    }

    private static ResponseEntity<ApiErrorResponse> jsonError(HttpStatus status, String slug) {
        return ResponseEntity.status(status).body(ApiErrorResponse.of(slug));
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            String first = (comma >= 0 ? forwarded.substring(0, comma) : forwarded).trim();
            if (!first.isEmpty()) {
                return first.length() > 45 ? first.substring(0, 45) : first;
            }
        }
        String remote = request.getRemoteAddr();
        if (remote == null) {
            return null;
        }
        return remote.length() > 45 ? remote.substring(0, 45) : remote;
    }
}
