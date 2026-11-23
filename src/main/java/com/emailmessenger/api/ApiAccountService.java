package com.emailmessenger.api;

import com.emailmessenger.auth.AccountService;
import com.emailmessenger.auth.SignedInDeviceService;
import com.emailmessenger.billing.PlanLimitService;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** docs/API.md §5.15–5.20, over the same services as the website's /account page. */
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

    @Transactional
    ApiDtos.MeResponse updateProfile(ApiPrincipal principal, JsonNode body) {
        // JsonNode rather than a record: an absent key must be an error, while null clears the name.
        JsonNode name = body == null || !body.isObject() ? null : body.get("displayName");
        if (name == null || !(name.isNull() || name.isTextual())) {
            throw validation("'displayName' is required: a string, or null to clear it.");
        }
        User user = requireUser(principal);
        if (accounts.changeDisplayName(user, name.isNull() ? null : name.textValue())
                == AccountService.DisplayNameChangeOutcome.TOO_LONG) {
            throw validation("Display name must be at most " + AccountService.MAX_DISPLAY_NAME_LENGTH
                    + " characters.");
        }
        return ApiDtos.MeResponse.of(user, planLimits.currentPlan(user));
    }

    @Transactional
    void changePassword(ApiPrincipal principal, ApiDtos.ChangePasswordRequest request) {
        if (request == null || request.currentPassword() == null || request.newPassword() == null) {
            throw validation("'currentPassword' and 'newPassword' are required.");
        }
        User user = requireUser(principal);
        switch (accounts.changePassword(user, request.currentPassword(), request.newPassword(),
                principal.tokenId())) {
            case OK -> { }
            case NEW_TOO_SHORT -> throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "password_too_short",
                    "New password must be at least 8 characters.");
            case CURRENT_INCORRECT -> throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "current_password_incorrect", "That isn't your current password.");
        }
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

    private User requireUser(ApiPrincipal principal) {
        return users.findById(principal.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized", "Sign in again."));
    }

    private static ApiException validation(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "validation_failed", message);
    }
}
