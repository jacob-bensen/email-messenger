package com.emailmessenger.api;

import com.emailmessenger.auth.ApiTokenService;
import com.emailmessenger.auth.ClientIp;
import com.emailmessenger.auth.UserService;
import com.emailmessenger.billing.PlanLimitService;
import com.emailmessenger.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1")
class AuthApiController {

    record LoginRequest(@NotBlank @Size(max = 254) String email,
                        @NotBlank @Size(max = 200) String password) {}

    record LoginResponse(String token, Instant expiresAt, UserDto user) {}

    private final ApiTokenService tokens;
    private final UserService userService;
    private final PlanLimitService planLimits;

    AuthApiController(ApiTokenService tokens, UserService userService, PlanLimitService planLimits) {
        this.tokens = tokens;
        this.userService = userService;
        this.planLimits = planLimits;
    }

    @PostMapping("/auth/login")
    LoginResponse login(@Valid @RequestBody LoginRequest body, HttpServletRequest request) {
        ApiTokenService.IssuedToken issued =
                tokens.login(body.email(), body.password(), ClientIp.from(request));
        return new LoginResponse(issued.token(), issued.expiresAt(), dto(issued.user()));
    }

    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(HttpServletRequest request) {
        tokens.revoke(ApiTokenService.bearerToken(request));
    }

    @GetMapping("/me")
    UserDto me(Principal principal) {
        return dto(userService.requireByEmail(principal.getName()));
    }

    private UserDto dto(User user) {
        return UserDto.from(user, planLimits.currentPlan(user));
    }
}
