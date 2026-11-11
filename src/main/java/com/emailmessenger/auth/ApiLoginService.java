package com.emailmessenger.auth;

import com.emailmessenger.domain.User;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

/**
 * Password login for the mobile REST API. Credentials go through the same
 * {@link AuthenticationManager} as the web form, so the audit listeners
 * record the same LOGIN_SUCCESS / LOGIN_FAILURE events and the
 * {@link LoginThrottleService} lockout is shared between web and API — a
 * locked account can't be probed through the other door.
 */
@Service
public class ApiLoginService {

    public enum Status { OK, INVALID_CREDENTIALS, LOCKED }

    public record Result(Status status, User user, ApiTokenService.IssuedToken token) {
        static Result failed(Status status) {
            return new Result(status, null, null);
        }
    }

    private final AuthenticationConfiguration authenticationConfiguration;
    private final LoginThrottleService throttle;
    private final UserService users;
    private final ApiTokenService tokens;

    ApiLoginService(AuthenticationConfiguration authenticationConfiguration,
                    LoginThrottleService throttle,
                    UserService users,
                    ApiTokenService tokens) {
        this.authenticationConfiguration = authenticationConfiguration;
        this.throttle = throttle;
        this.users = users;
        this.tokens = tokens;
    }

    public Result login(String email, String password, String deviceName) {
        String normalized = UserService.normalizeEmail(email);
        if (throttle.isLocked(normalized, ClientIp.fromCurrentRequest())) {
            return Result.failed(Status.LOCKED);
        }
        Authentication authentication;
        try {
            authentication = authenticationManager().authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(normalized, password));
        } catch (AuthenticationException e) {
            // Unknown email, wrong password and disabled account are deliberately
            // indistinguishable to the caller.
            return Result.failed(Status.INVALID_CREDENTIALS);
        }
        User user = users.requireByEmail(authentication.getName());
        return new Result(Status.OK, user, tokens.issue(user, deviceName));
    }

    private AuthenticationManager authenticationManager() {
        try {
            return authenticationConfiguration.getAuthenticationManager();
        } catch (Exception e) {
            throw new IllegalStateException("No AuthenticationManager available", e);
        }
    }
}
