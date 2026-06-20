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
        if (summaries.isEmpty()) {
            return summaries.map(s -> null);
        }
        List<String> keys = summaries.getContent().stream()
                .map(ConversationSummaryRow::getConversationKey).toList();

        Map<String, ConversationPreviewRow> previews = firstByKey(
                threads.latestMessagePerConversation(owner, keys));
        Map<String, List<Member>> membersByKey =
                membersByKey(owner, keys);

        return summaries.map(s -> toItem(s, previews.get(s.getConversationKey()),
                membersByKey.getOrDefault(s.getConversationKey(), List.of())));
    }

    private ConversationListItem toItem(ConversationSummaryRow summary,
                                        ConversationPreviewRow preview, List<Member> members) {
        List<String> labels = members.stream().map(Member::label).toList();
        String title = ConversationLabels.title(labels);
        String initials = members.isEmpty() ? "?"
                : ConversationLabels.initials(members.get(0).displayName(), members.get(0).email());
        return new ConversationListItem(
                summary.getConversationKey(), title, initials,
                members.size() > 1, members.size(),
                preview(preview), summary.getLastActivity(),
                summary.getUnreadCount() > 0, summary.getThreadCount());
    }

    /** Members per conversation: senders + non-Bcc recipients, owner excluded, deduped. */
    private Map<String, List<Member>> membersByKey(User owner, List<String> keys) {
        Set<String> ownerAddresses = ownerAddressService.addressesFor(owner);
        Map<String, LinkedHashMap<String, Member>> acc = new LinkedHashMap<>();
        accumulate(acc, threads.sendersForConversations(owner, keys), ownerAddresses);
        accumulate(acc, threads.recipientsForConversations(owner, keys, RecipientType.BCC), ownerAddresses);

        Map<String, List<Member>> out = new LinkedHashMap<>();
        for (Map.Entry<String, LinkedHashMap<String, Member>> e : acc.entrySet()) {
            List<Member> members = new ArrayList<>(e.getValue().values());
            members.sort((a, b) -> a.label().compareToIgnoreCase(b.label()));
            out.put(e.getKey(), members);
        }
        return out;
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
