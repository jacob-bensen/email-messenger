package com.emailmessenger.web;

import com.emailmessenger.domain.RecipientType;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.OwnerAddressService;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.repository.EmailThreadRepository.ConversationParticipantRow;
import com.emailmessenger.repository.EmailThreadRepository.ConversationPreviewRow;
import com.emailmessenger.repository.EmailThreadRepository.ConversationSummaryRow;
import com.emailmessenger.service.ConversationLabels;
import com.emailmessenger.service.ConversationListItem;
import org.jsoup.Jsoup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Assembles the texting-style chats list: one row per conversation (a person or
 * group), ordered most-recently-active first, with a one-line preview and the
 * member labels. Conversations are the threads sharing a {@code conversationKey}
 * collapsed together — see {@link com.emailmessenger.service.ConversationKeyService}.
 */
@Service
public class ConversationListService {

    private static final int PREVIEW_MAX = 120;

    /** Triage lenses for the Dashboard. */
    public enum ChatFilter { ALL, UNREAD, AWAITING, ATTACHMENTS }

    /** Per-tile conversation counts for the current scope, shown on the Dashboard. */
    public record ChatCounts(long all, long unread, long awaiting, long attachments) {}

    private final EmailThreadRepository threads;
    private final OwnerAddressService ownerAddressService;

    ConversationListService(EmailThreadRepository threads, OwnerAddressService ownerAddressService) {
        this.threads = threads;
        this.ownerAddressService = ownerAddressService;
    }

    @Transactional(readOnly = true)
    public Page<ConversationListItem> list(User owner, Pageable pageable) {
        return list(owner, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ConversationListItem> list(User owner, String query, Pageable pageable) {
        String q = query == null ? "" : query.trim();
        Page<ConversationSummaryRow> summaries = q.isEmpty()
                ? threads.conversationSummaries(owner, pageable)
                : threads.conversationSummariesMatching(owner, q, pageable);
        return assemble(owner, summaries, false);
    }

    /**
     * Conversations scoped to a single connected account (its login address),
     * so each mailbox's chats stay separate and are never merged into one list.
     * {@code mailboxAddress} is the account's address; it is matched against the
     * owner-side participant (sender or recipient) of each conversation's threads.
     */
    @Transactional(readOnly = true)
    public Page<ConversationListItem> list(User owner, String mailboxAddress, String query, Pageable pageable) {
        String mbox = mailboxAddress == null ? "" : mailboxAddress.trim().toLowerCase(Locale.ROOT);
        String q = query == null ? "" : query.trim();
        Page<ConversationSummaryRow> summaries = q.isEmpty()
                ? threads.conversationSummariesForMailbox(owner, mbox, pageable)
                : threads.conversationSummariesMatchingForMailbox(owner, mbox, q, pageable);
        return assemble(owner, summaries, false);
    }

    /**
     * The Dashboard list: optional account scope ({@code mailboxAddress} null =
     * all connected accounts merged into one cross-account feed), optional
     * free-text query, and a triage {@link ChatFilter}. When unscoped (the Hub),
     * each row is tagged with the account(s) it belongs to.
     */
    @Transactional(readOnly = true)
    public Page<ConversationListItem> list(User owner, String mailboxAddress, String query,
                                           ChatFilter filter, Pageable pageable) {
        String mbox = blankToNull(mailboxAddress, true);
        String q = blankToNull(query, false);
        ChatFilter f = filter == null ? ChatFilter.ALL : filter;
        Page<ConversationSummaryRow> summaries = threads.conversationSummariesFiltered(
                owner, mbox, q,
                f == ChatFilter.UNREAD, f == ChatFilter.ATTACHMENTS, f == ChatFilter.AWAITING,
                pageable);
        // Only the cross-account Hub needs per-row account tags.
        return assemble(owner, summaries, mbox == null);
    }

    /** Conversation counts per triage tile for the given scope (null = all accounts). */
    @Transactional(readOnly = true)
    public ChatCounts counts(User owner, String mailboxAddress) {
        String mbox = blankToNull(mailboxAddress, true);
        return new ChatCounts(
                threads.countConversationsFiltered(owner, mbox, false, false, false),
                threads.countConversationsFiltered(owner, mbox, true, false, false),
                threads.countConversationsFiltered(owner, mbox, false, false, true),
                threads.countConversationsFiltered(owner, mbox, false, true, false));
    }

    private static String blankToNull(String value, boolean lower) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return lower ? trimmed.toLowerCase(Locale.ROOT) : trimmed;
    }

    private Page<ConversationListItem> assemble(User owner, Page<ConversationSummaryRow> summaries,
                                               boolean includeAccounts) {
        if (summaries.isEmpty()) {
            return summaries.map(s -> null);
        }
        List<String> keys = summaries.getContent().stream()
                .map(ConversationSummaryRow::getConversationKey).toList();

        Map<String, ConversationPreviewRow> previews = firstByKey(
                threads.latestMessagePerConversation(owner, keys));
        Set<String> ownerAddresses = ownerAddressService.addressesFor(owner);
        List<ConversationParticipantRow> senders = threads.sendersForConversations(owner, keys);
        List<ConversationParticipantRow> recipients =
                threads.recipientsForConversations(owner, keys, RecipientType.BCC);
        Map<String, List<Member>> membersByKey = membersByKey(senders, recipients, ownerAddresses);
        Map<String, List<String>> accountsByKey = includeAccounts
                ? accountsByKey(senders, recipients, ownerAddresses) : Map.of();

        return summaries.map(s -> toItem(s, previews.get(s.getConversationKey()),
                membersByKey.getOrDefault(s.getConversationKey(), List.of()),
                accountsByKey.getOrDefault(s.getConversationKey(), List.of())));
    }

    private ConversationListItem toItem(ConversationSummaryRow summary, ConversationPreviewRow preview,
                                        List<Member> members, List<String> accounts) {
        List<String> labels = members.stream().map(Member::label).toList();
        String title = ConversationLabels.title(labels);
        String initials = members.isEmpty() ? "?"
                : ConversationLabels.initials(members.get(0).displayName(), members.get(0).email());
        return new ConversationListItem(
                summary.getConversationKey(), title, initials,
                members.size() > 1, members.size(),
                preview(preview), summary.getLastActivity(),
                summary.getUnreadCount() > 0, summary.getThreadCount(), accounts,
                senderEmail(preview, members));
    }

    /**
     * Email shown in grey after the name: the latest message's sender when it was
     * received, otherwise the primary correspondent (so an outbound last message
     * doesn't surface your own address).
     */
    private static String senderEmail(ConversationPreviewRow preview, List<Member> members) {
        if (preview != null && !preview.getOutbound()
                && preview.getSenderEmail() != null && !preview.getSenderEmail().isBlank()) {
            return preview.getSenderEmail();
        }
        return members.isEmpty() ? null : members.get(0).email();
    }

    /** Members per conversation: senders + non-Bcc recipients, owner excluded, deduped. */
    private Map<String, List<Member>> membersByKey(List<ConversationParticipantRow> senders,
                                                   List<ConversationParticipantRow> recipients,
                                                   Set<String> ownerAddresses) {
        Map<String, LinkedHashMap<String, Member>> acc = new LinkedHashMap<>();
        accumulate(acc, senders, ownerAddresses);
        accumulate(acc, recipients, ownerAddresses);

        Map<String, List<Member>> out = new LinkedHashMap<>();
        for (Map.Entry<String, LinkedHashMap<String, Member>> e : acc.entrySet()) {
            List<Member> members = new ArrayList<>(e.getValue().values());
            members.sort((a, b) -> a.label().compareToIgnoreCase(b.label()));
            out.put(e.getKey(), members);
        }
        return out;
    }

    /** The user's own account address(es) that appear in each conversation. */
    private Map<String, List<String>> accountsByKey(List<ConversationParticipantRow> senders,
                                                    List<ConversationParticipantRow> recipients,
                                                    Set<String> ownerAddresses) {
        Map<String, LinkedHashSet<String>> acc = new LinkedHashMap<>();
        collectOwnerAddresses(acc, senders, ownerAddresses);
        collectOwnerAddresses(acc, recipients, ownerAddresses);
        Map<String, List<String>> out = new LinkedHashMap<>();
        for (Map.Entry<String, LinkedHashSet<String>> e : acc.entrySet()) {
            List<String> addresses = new ArrayList<>(e.getValue());
            addresses.sort(String.CASE_INSENSITIVE_ORDER);
            out.put(e.getKey(), addresses);
        }
        return out;
    }

    private static void collectOwnerAddresses(Map<String, LinkedHashSet<String>> acc,
                                              List<ConversationParticipantRow> rows,
                                              Set<String> ownerAddresses) {
        for (ConversationParticipantRow row : rows) {
            if (row.getEmail() == null) {
                continue;
            }
            String email = row.getEmail().trim().toLowerCase(Locale.ROOT);
            if (!email.isEmpty() && ownerAddresses.contains(email)) {
                acc.computeIfAbsent(row.getConversationKey(), k -> new LinkedHashSet<>()).add(email);
            }
        }
    }

    private static void accumulate(Map<String, LinkedHashMap<String, Member>> acc,
                                   List<ConversationParticipantRow> rows, Set<String> ownerAddresses) {
        for (ConversationParticipantRow row : rows) {
            if (row.getEmail() == null) {
                continue;
            }
            String email = row.getEmail().trim().toLowerCase(Locale.ROOT);
            if (email.isEmpty() || ownerAddresses.contains(email)) {
                continue;
            }
            LinkedHashMap<String, Member> members =
                    acc.computeIfAbsent(row.getConversationKey(), k -> new LinkedHashMap<>());
            Member existing = members.get(email);
            boolean named = row.getDisplayName() != null && !row.getDisplayName().isBlank();
            if (existing == null
                    || (existing.displayName() == null || existing.displayName().isBlank()) && named) {
                members.put(email, new Member(row.getEmail(), row.getDisplayName()));
            }
        }
    }

    private static Map<String, ConversationPreviewRow> firstByKey(List<ConversationPreviewRow> rows) {
        Map<String, ConversationPreviewRow> out = new LinkedHashMap<>();
        for (ConversationPreviewRow row : rows) {
            out.putIfAbsent(row.getConversationKey(), row);
        }
        return out;
    }

    private static String preview(ConversationPreviewRow row) {
        if (row == null) {
            return "";
        }
        String text;
        if (row.getBodyHtml() != null && !row.getBodyHtml().isBlank()) {
            text = Jsoup.parse(row.getBodyHtml()).text();
        } else {
            text = row.getBodyPlain() == null ? "" : row.getBodyPlain();
        }
        text = text.replaceAll("\\s+", " ").trim();
        if (text.length() > PREVIEW_MAX) {
            text = text.substring(0, PREVIEW_MAX).trim() + "…";
        }
        return row.getOutbound() && !text.isEmpty() ? "You: " + text : text;
    }

    private record Member(String email, String displayName) {
        String label() {
            return (displayName != null && !displayName.isBlank()) ? displayName.trim() : email;
        }
    }
}
