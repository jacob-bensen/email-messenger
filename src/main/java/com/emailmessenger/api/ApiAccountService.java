package com.emailmessenger.api;

import com.emailmessenger.auth.AccountService;
import com.emailmessenger.auth.SignedInDeviceService;
import com.emailmessenger.billing.PlanLimitService;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** docs/API.md §5.15–5.18, over the same services as the website's /account page. */
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
        User user = users.findById(principal.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized", "Sign in again."));
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
}
