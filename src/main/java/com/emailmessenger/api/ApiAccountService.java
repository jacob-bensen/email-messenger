package com.emailmessenger.api;

import com.emailmessenger.auth.AccountService;
import com.emailmessenger.auth.SignedInDeviceService;
import com.emailmessenger.billing.PlanLimitService;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** docs/API.md §5.15–5.21, over the same services as the website's /account page. */
@Service
class ApiAccountService {

    private final UserRepository users;
    private final PlanLimitService planLimits;
    private final AccountService accounts;
    private final SignedInDeviceService devices;

    ApiAccountService(UserRepository users, PlanLimitService planLimits, AccountService accounts,
                      SignedInDeviceService devices) {
        this.users = users;
        this.planLimits = planLimits;
        this.accounts = accounts;
        this.devices = devices;
    }

    @Transactional(readOnly = true)
    ApiDtos.AccountResponse account(ApiPrincipal principal) {
        User user = requireUser(principal);
        return new ApiDtos.AccountResponse(ApiDtos.MeResponse.of(user, planLimits.currentPlan(user)),
                ApiDtos.BillingResponse.of(accounts.billingSummary(user)));
    }

    ApiDtos.DeviceList devices(ApiPrincipal principal) {
        return new ApiDtos.DeviceList(devices.list(principal.userId(), principal.tokenId()).stream()
                .map(ApiDtos.DeviceResponse::of)
                .toList());
    }

    void revoke(ApiPrincipal principal, long deviceId) {
        if (!devices.revoke(principal.userId(), deviceId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "not_found", "That device is not signed in.");
        }
    }

    ApiDtos.SignOutOthersResponse signOutOthers(ApiPrincipal principal) {
        return new ApiDtos.SignOutOthersResponse(devices.revokeAllExcept(principal.userId(), principal.tokenId()));
    }

    @Transactional
    ApiDtos.MeResponse changeDisplayName(ApiPrincipal principal, String displayName) {
        User user = requireUser(principal);
        if (accounts.changeDisplayName(user, displayName) == AccountService.DisplayNameChangeOutcome.TOO_LONG) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "validation_failed",
                    "Display name can be at most " + AccountService.MAX_DISPLAY_NAME_LENGTH + " characters.");
        }
        return me(user);
    }

    @Transactional
    void changePassword(ApiPrincipal principal, String currentPassword, String newPassword) {
        switch (accounts.changePassword(requireUser(principal), currentPassword, newPassword)) {
            case OK -> { }
            case NEW_TOO_SHORT -> throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "password_too_short",
                    "New password must be at least 8 characters.");
            case CURRENT_INCORRECT -> throw currentPasswordIncorrect();
        }
    }

    @Transactional
    ApiDtos.MeResponse changeEmail(ApiPrincipal principal, String currentPassword, String newEmail) {
        User user = requireUser(principal);
        return switch (accounts.changeEmail(user, currentPassword, newEmail)) {
            case OK -> me(user);
            case CURRENT_INCORRECT -> throw currentPasswordIncorrect();
            case EMAIL_INVALID -> throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "email_invalid",
                    "That doesn't look like a valid email address.");
            case NO_CHANGE -> throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "email_unchanged",
                    "That's already your email address.");
            case EMAIL_TAKEN -> throw new ApiException(HttpStatus.CONFLICT, "email_taken",
                    "An account with that email already exists.");
        };
    }

    private User requireUser(ApiPrincipal principal) {
        return users.findById(principal.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized", "Sign in again."));
    }

    private ApiDtos.MeResponse me(User user) {
        return ApiDtos.MeResponse.of(user, planLimits.currentPlan(user));
    }

    private static ApiException currentPasswordIncorrect() {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "current_password_incorrect",
                "That isn't your current password.");
    }
}
