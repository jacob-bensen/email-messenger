package com.emailmessenger.api;

import com.emailmessenger.auth.ApiTokenService;
import com.emailmessenger.auth.ClientIp;
import com.emailmessenger.auth.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1")
class ApiAuthController {

    private final ApiAuthService authService;
    private final ApiTokenService tokens;
    private final UserService userService;

    ApiAuthController(ApiAuthService authService, ApiTokenService tokens, UserService userService) {
        this.authService = authService;
        this.tokens = tokens;
        this.userService = userService;
    }

    @PostMapping("/auth/login")
    LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return authService.login(request, ClientIp.from(http));
    }

    /** Revokes the token this request was made with. */
    @PostMapping("/auth/logout")
    ResponseEntity<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        tokens.revoke(authorization.substring(authorization.indexOf(' ') + 1).trim());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    UserDto me(Principal principal) {
        return authService.me(userService.requireByEmail(principal.getName()));
    }
}
