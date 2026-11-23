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
        User user = user(principal);
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
    ApiDtos.MeResponse updateProfile(ApiPrincipal principal, ApiDtos.ProfileRequest request) {
        User user = user(principal);
        if (accounts.changeDisplayName(user, request.displayName()) == AccountService.DisplayNameOutcome.TOO_LONG) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "validation_failed",
                    "Display name must be " + AccountService.MAX_DISPLAY_NAME_LENGTH + " characters or fewer.");
        }
        return ApiDtos.MeResponse.of(user, planLimits.currentPlan(user));
    }

    @Transactional
    ApiDtos.ChangePasswordResponse changePassword(ApiPrincipal principal, ApiDtos.ChangePasswordRequest request) {
        AccountService.PasswordChange change = accounts.changePassword(user(principal),
                request.currentPassword(), request.newPassword(), principal.tokenId());
        return switch (change.outcome()) {
            case OK -> new ApiDtos.ChangePasswordResponse(change.revokedDevices());
            case CURRENT_INCORRECT -> throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "current_password_incorrect", "That isn't your current password.");
            case NEW_TOO_SHORT -> throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "password_too_short", "The new password must be at least 8 characters.");
            case NEW_TOO_LONG -> throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "password_too_long", "The new password is too long (72 bytes at most).");
        };
    }

    @Transactional
    ApiDtos.MeResponse changeEmail(ApiPrincipal principal, ApiDtos.ChangeEmailRequest request) {
        User user = user(principal);
        return switch (accounts.changeEmail(user, request.currentPassword(), request.newEmail())) {
            case OK -> ApiDtos.MeResponse.of(user, planLimits.currentPlan(user));
            case CURRENT_INCORRECT -> throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "current_password_incorrect", "That isn't your current password.");
            case EMAIL_INVALID -> throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "email_invalid", "Enter a valid email address.");
            case NO_CHANGE -> throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "email_unchanged", "That is already your email address.");
            case EMAIL_TAKEN -> throw new ApiException(HttpStatus.CONFLICT,
                    "email_taken", "Another account already uses that email address.");
        };
    }

    private User user(ApiPrincipal principal) {
        return users.findById(principal.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized", "Sign in again."));
    }
}
