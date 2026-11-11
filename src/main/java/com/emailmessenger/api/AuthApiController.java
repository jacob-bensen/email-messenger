package com.emailmessenger.api;

import com.emailmessenger.auth.ApiLoginService;
import com.emailmessenger.auth.ApiTokenService;
import com.emailmessenger.auth.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1")
class AuthApiController {

    record LoginRequest(
            @NotBlank @Size(max = 254) String email,
            @NotBlank @Size(max = 200) String password,
            @Size(max = 100) String deviceName) {}

    record LoginResponse(String token, Instant expiresAt, UserDto user) {}

    private final ApiLoginService loginService;
    private final ApiTokenService tokenService;
    private final UserService userService;
    private final CurrentUserService currentUserService;

    AuthApiController(ApiLoginService loginService,
                      ApiTokenService tokenService,
                      UserService userService,
                      CurrentUserService currentUserService) {
        this.loginService = loginService;
        this.tokenService = tokenService;
        this.userService = userService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/auth/login")
    LoginResponse login(@Valid @RequestBody LoginRequest request) {
        ApiLoginService.LoginResult result =
                loginService.login(request.email(), request.password(), request.deviceName());
        return new LoginResponse(
                result.token().token(),
                ApiTime.toInstant(result.token().expiresAt()),
                currentUserService.describe(result.user()));
    }

    @PostMapping("/auth/logout")
    ResponseEntity<Void> logout(HttpServletRequest request) {
        tokenService.revoke(ApiTokenService.bearerToken(request));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    UserDto me(Principal principal) {
        return currentUserService.describe(userService.requireByEmail(principal.getName()));
    }
}
