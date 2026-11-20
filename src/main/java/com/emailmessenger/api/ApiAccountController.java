package com.emailmessenger.api;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/account")
class ApiAccountController {

    private final ApiAccountService account;

    ApiAccountController(ApiAccountService account) {
        this.account = account;
    }

    @GetMapping
    ApiDtos.AccountResponse account(@AuthenticationPrincipal ApiPrincipal principal) {
        return account.account(principal);
    }

    @GetMapping("/devices")
    ApiDtos.DeviceList devices(@AuthenticationPrincipal ApiPrincipal principal) {
        return account.devices(principal);
    }

    @DeleteMapping("/devices/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void revoke(@AuthenticationPrincipal ApiPrincipal principal, @PathVariable long id) {
        account.revoke(principal, id);
    }

    @PostMapping("/devices/sign-out-others")
    ApiDtos.SignOutOthersResponse signOutOthers(@AuthenticationPrincipal ApiPrincipal principal) {
        return account.signOutOthers(principal);
    }

    @PutMapping("/display-name")
    ApiDtos.MeResponse changeDisplayName(@AuthenticationPrincipal ApiPrincipal principal,
                                         @Valid @RequestBody ApiDtos.DisplayNameRequest request) {
        return account.changeDisplayName(principal, request.displayName());
    }

    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changePassword(@AuthenticationPrincipal ApiPrincipal principal,
                        @Valid @RequestBody ApiDtos.ChangePasswordRequest request) {
        account.changePassword(principal, request.currentPassword(), request.newPassword());
    }

    @PostMapping("/email")
    ApiDtos.MeResponse changeEmail(@AuthenticationPrincipal ApiPrincipal principal,
                                   @Valid @RequestBody ApiDtos.ChangeEmailRequest request) {
        return account.changeEmail(principal, request.currentPassword(), request.newEmail());
    }
}
