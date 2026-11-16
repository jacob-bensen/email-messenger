package com.emailmessenger.api;

import com.emailmessenger.auth.ClientIp;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
class ApiAuthController {

    private final ApiAuthService auth;
    private final ApiTokenService tokens;

    ApiAuthController(ApiAuthService auth, ApiTokenService tokens) {
        this.auth = auth;
        this.tokens = tokens;
    }

    @PostMapping("/auth/login")
    ApiDtos.LoginResponse login(@Valid @RequestBody ApiDtos.LoginRequest request, HttpServletRequest http) {
        return auth.login(request, ClientIp.from(http));
    }

    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@AuthenticationPrincipal ApiPrincipal principal) {
        tokens.revoke(principal.tokenId());
    }

    @GetMapping("/me")
    ApiDtos.MeResponse me(@AuthenticationPrincipal ApiPrincipal principal) {
        return auth.me(principal);
    }
}
