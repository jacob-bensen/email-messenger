package com.emailmessenger.api;

import com.emailmessenger.billing.PlanLimits;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.User;
import com.emailmessenger.service.ConversationListItem;
import com.emailmessenger.web.ConversationListService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
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

    record ConversationPage(List<ConversationRow> items, int page, int size, long totalItems,
                            boolean hasNext, CountsResponse counts) {

        static ConversationPage of(Page<ConversationListItem> rows, ConversationListService.ChatCounts counts) {
            return new ConversationPage(rows.getContent().stream().map(ConversationRow::of).toList(),
                    rows.getNumber(), rows.getSize(), rows.getTotalElements(), rows.hasNext(),
                    new CountsResponse(counts.all(), counts.unread(), counts.awaiting(), counts.attachments()));
        }
    }

    record ConversationRow(String key, String title, String initials, boolean group, int memberCount,
                           String preview, Instant lastActivityAt, boolean unread, long threadCount,
                           List<String> accounts, String senderEmail) {

        static ConversationRow of(ConversationListItem item) {
            return new ConversationRow(item.key(), item.title(), item.initials(), item.group(),
                    item.memberCount(), item.preview() == null ? "" : item.preview(),
                    item.lastActivity() == null ? null : item.lastActivity().toInstant(ZoneOffset.UTC),
                    item.unread(), item.threadCount(),
                    item.accounts() == null ? List.of() : item.accounts(), item.senderEmail());
        }
    }

    record CountsResponse(long all, long unread, long awaiting, long attachments) {}
}
