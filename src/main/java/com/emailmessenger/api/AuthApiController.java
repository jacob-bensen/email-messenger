package com.emailmessenger.api;

import com.emailmessenger.auth.ApiLoginService;
import com.emailmessenger.auth.ApiTokenService;
import com.emailmessenger.auth.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/api/v1")
class AuthApiController {

    private final ApiLoginService loginService;
    private final ApiTokenService tokens;
    private final UserService users;

    AuthApiController(ApiLoginService loginService, ApiTokenService tokens, UserService users) {
        this.loginService = loginService;
        this.tokens = tokens;
        this.users = users;
    }

    @PostMapping("/auth/login")
    LoginResponse login(@Valid @RequestBody LoginRequest request) {
        ApiLoginService.Result result =
                loginService.login(request.email(), request.password(), request.deviceName());
        return switch (result.status()) {
            case OK -> new LoginResponse(
                    result.token().plainToken(),
                    result.token().expiresAt().toInstant(ZoneOffset.UTC),
                    UserResponse.of(result.user()));
            case LOCKED -> throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "too_many_attempts",
                    "Too many failed sign-in attempts. Try again later.");
            case INVALID_CREDENTIALS -> throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "invalid_credentials", "Incorrect email or password.");
        };
    }

    @PostMapping("/auth/logout")
    ResponseEntity<Void> logout(Authentication authentication) {
        // The bearer filter stores the presented token as the credentials.
        if (authentication.getCredentials() instanceof String token) {
            tokens.revoke(token);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    UserResponse me(Principal principal) {
        return UserResponse.of(users.requireByEmail(principal.getName()));
    }
}
