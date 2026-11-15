package com.emailmessenger.api;

import com.emailmessenger.billing.PlanLimits;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Locale;

/** Request/response bodies of docs/API.md §5. Field names are the contract. */
final class ApiDtos {

    private ApiDtos() {}

    record LoginRequest(
            @NotBlank String email,
            @NotBlank String password,
            @Size(max = ApiTokenService.MAX_DEVICE_NAME_LENGTH) String deviceName) {}

    record LoginResponse(String token, String tokenType, Instant expiresAt, MeResponse user) {}

    record MeResponse(long id, String email, String displayName, boolean emailVerified,
                      String plan, LimitsResponse limits) {

        static MeResponse of(User user, Plan plan) {
            return new MeResponse(user.getId(), user.getEmail(), user.getDisplayName(),
                    user.isEmailVerified(), plan.name().toLowerCase(Locale.ROOT),
                    LimitsResponse.of(PlanLimits.forPlan(plan)));
        }
    }

    /** {@code null} means unlimited. */
    record LimitsResponse(Long mailboxes, Long threads, Long savedSearches) {

        static LimitsResponse of(PlanLimits limits) {
            return new LimitsResponse(cap(limits.mailboxes()), cap(limits.threads()), cap(limits.savedSearches()));
        }

        private static Long cap(long value) {
            return value == PlanLimits.UNLIMITED ? null : value;
        }
    }
}
