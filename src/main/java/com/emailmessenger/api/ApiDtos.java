package com.emailmessenger.api;

import com.emailmessenger.auth.AccountBillingSummary;
import com.emailmessenger.auth.SignedInDevice;
import com.emailmessenger.billing.PlanLimits;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.User;
import com.emailmessenger.domain.Attachment;
import com.emailmessenger.domain.AuthType;
import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.service.BubbleMessage;
import com.emailmessenger.service.ChatConversation;
import com.emailmessenger.service.ChatMember;
import com.emailmessenger.service.ConversationListItem;
import com.emailmessenger.service.HtmlText;
import com.emailmessenger.service.SenderBubbleRun;
import com.emailmessenger.service.SenderView;
import com.emailmessenger.web.ChatSendService;
import com.emailmessenger.web.ConversationListService;
import com.emailmessenger.web.MailboxProvider;
import com.emailmessenger.web.MailboxView;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
                      boolean passwordSet, String plan, LimitsResponse limits) {

        static MeResponse of(User user, Plan plan) {
            return new MeResponse(user.getId(), user.getEmail(), user.getDisplayName(),
                    user.isEmailVerified(), user.isPasswordSet(), plan.name().toLowerCase(Locale.ROOT),
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

    record ConversationDetail(String key, String title, String initials, boolean group, long threadCount,
                              int messageCount, List<MemberResponse> members, List<RunResponse> runs) {

        static ConversationDetail of(ChatConversation chat) {
            return new ConversationDetail(chat.key(), chat.title(), chat.initials(), chat.isGroup(),
                    chat.threadCount(), chat.messageCount(),
                    chat.members().stream().map(MemberResponse::of).toList(),
                    chat.runs().stream().map(RunResponse::of).toList());
        }
    }

    record MemberResponse(String email, String displayName, String initials, String label) {

        static MemberResponse of(ChatMember member) {
            return new MemberResponse(member.email(), blankToNull(member.displayName()),
                    member.initials(), member.label());
        }
    }

    record RunResponse(boolean outbound, SenderResponse sender, ThreadResponse thread,
                       List<MessageResponse> messages) {

        static RunResponse of(SenderBubbleRun run) {
            SenderView s = run.sender();
            return new RunResponse(run.outbound(),
                    new SenderResponse(s.email(), blankToNull(s.displayName()), s.initials()),
                    new ThreadResponse(run.thread().threadId(), run.thread().subject()),
                    run.messages().stream().map(MessageResponse::of).toList());
        }
    }

    record SenderResponse(String email, String displayName, String initials) {}

    record ThreadResponse(Long id, String subject) {}

    record MessageResponse(Long id, Instant sentAt, String bodyHtml, String bodyText, Long replyToId,
                           List<AttachmentResponse> attachments) {

        static MessageResponse of(BubbleMessage message) {
            String html = message.bodyHtml() == null ? "" : message.bodyHtml();
            return new MessageResponse(message.messageId(),
                    message.sentAt() == null ? null : message.sentAt().toInstant(ZoneOffset.UTC),
                    html, HtmlText.toPlainText(html), message.replyToMessageId(),
                    message.attachments().stream().map(AttachmentResponse::of).toList());
        }
    }

    record AttachmentResponse(String filename, String mimeType, Long sizeBytes) {

        static AttachmentResponse of(Attachment attachment) {
            return new AttachmentResponse(attachment.getFilename(), attachment.getMimeType(),
                    attachment.getSizeBytes());
        }
    }

    record SendMessageRequest(String body, String subject, Boolean asReply) {}

    record NewConversationRequest(List<String> to, String subject, String body) {}

    record SendResponse(String key, long threadId, long messageId) {

        static SendResponse of(ChatSendService.SendResult result) {
            return new SendResponse(result.conversationKey(), result.threadId(), result.messageId());
        }
    }

    record MailboxList(List<MailboxResponse> items, Long limit) {}

    record MailboxResponse(long id, String address, String host, int port, boolean ssl, String authType,
                           boolean isDefault, Instant lastSyncedAt, String lastSyncError, String errorHint,
                           boolean pollingSuspended) {

        static MailboxResponse of(MailAccount account, Long defaultAccountId) {
            return new MailboxResponse(account.getId(), account.getUsername(), account.getHost(),
                    account.getPort(), account.isSsl(),
                    account.getAuthType() == AuthType.XOAUTH2 ? "google" : "password",
                    account.getId().equals(defaultAccountId),
                    account.getLastSyncedAt() == null ? null : account.getLastSyncedAt().toInstant(ZoneOffset.UTC),
                    account.getLastSyncError(), MailboxView.errorHint(account.getLastSyncError()),
                    account.isPollingSuspended());
        }
    }

    record ProviderList(List<ProviderResponse> items) {}

    record ProviderResponse(String id, String name, String host, int port, boolean ssl, String appPasswordUrl,
                            String help) {

        static ProviderResponse of(MailboxProvider provider) {
            return new ProviderResponse(provider.getSlug(), provider.getDisplayName(), provider.getHost(),
                    provider.getPort(), provider.isSsl(), provider.getAppPasswordUrl(),
                    provider.getAppPasswordHelp());
        }
    }

    /** Missing {@code port}/{@code ssl} take the website form's defaults. */
    record ConnectMailboxRequest(String host, Integer port, Boolean ssl, String username, String password) {}

    record SyncResponse(int imported, MailboxResponse mailbox) {}

    record AccountResponse(MeResponse user, BillingResponse billing) {}

    record BillingResponse(String label, LocalDate renewsOn, boolean trialing, boolean canceled) {

        static BillingResponse of(AccountBillingSummary summary) {
            return summary == null ? null
                    : new BillingResponse(summary.label(), summary.renewsOn(), summary.trialing(), summary.canceled());
        }
    }

    record DeviceList(List<DeviceResponse> items) {}

    record DeviceResponse(long id, String deviceName, Instant signedInAt, Instant lastUsedAt, Instant expiresAt,
                          boolean current) {

        static DeviceResponse of(SignedInDevice device) {
            return new DeviceResponse(device.id(), device.deviceName(), utc(device.signedInAt()),
                    utc(device.lastUsedAt()), utc(device.expiresAt()), device.current());
        }
    }

    record SignOutOthersResponse(int revoked) {}

    record ProfileRequest(String displayName) {}

    record ChangePasswordRequest(@NotNull String currentPassword, @NotNull String newPassword) {}

    record ChangePasswordResponse(int revokedDevices) {}

    record ChangeEmailRequest(@NotNull String currentPassword, @NotNull String newEmail) {}

    private static Instant utc(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
