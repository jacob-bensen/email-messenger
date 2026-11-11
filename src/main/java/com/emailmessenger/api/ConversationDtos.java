package com.emailmessenger.api;

import com.emailmessenger.domain.Attachment;
import com.emailmessenger.service.BubbleMessage;
import com.emailmessenger.service.ChatConversation;
import com.emailmessenger.service.ChatMember;
import com.emailmessenger.service.ConversationListItem;
import com.emailmessenger.service.SenderBubbleRun;
import com.emailmessenger.web.ConversationListService.ChatCounts;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Wire shapes for the conversations endpoints. Pure projections of the
 * service-layer view models the Thymeleaf pages render — no rules live here.
 */
final class ConversationDtos {

    private ConversationDtos() {}

    record ConversationPage(List<ConversationSummary> items, int page, int size,
                            long totalItems, int totalPages, boolean hasNext, ChatCounts counts) {

        static ConversationPage from(Page<ConversationListItem> page, ChatCounts counts) {
            return new ConversationPage(
                    page.getContent().stream().map(ConversationSummary::from).toList(),
                    page.getNumber(), page.getSize(), page.getTotalElements(),
                    page.getTotalPages(), page.hasNext(), counts);
        }
    }

    record ConversationSummary(String key, String title, String initials, boolean group,
                               int memberCount, String preview, Instant lastActivityAt,
                               boolean unread, long threadCount, List<String> accounts,
                               String senderEmail) {

        static ConversationSummary from(ConversationListItem item) {
            return new ConversationSummary(item.key(), item.title(), item.initials(), item.group(),
                    item.memberCount(), item.preview(), utc(item.lastActivity()), item.unread(),
                    item.threadCount(), item.accounts(), item.senderEmail());
        }
    }

    record ConversationDetail(String key, String title, String initials, boolean group,
                              long threadCount, int messageCount, List<Member> members,
                              List<Run> runs) {

        static ConversationDetail from(ChatConversation chat) {
            return new ConversationDetail(chat.key(), chat.title(), chat.initials(), chat.isGroup(),
                    chat.threadCount(), chat.messageCount(),
                    chat.members().stream().map(Member::from).toList(),
                    chat.runs().stream().map(Run::from).toList());
        }
    }

    record Member(String email, String displayName, String initials, String label,
                  String signatureHtml) {

        static Member from(ChatMember m) {
            return new Member(m.email(), m.displayName(), m.initials(), m.label(),
                    m.signatureHtml() == null ? "" : m.signatureHtml());
        }
    }

    record Sender(String email, String displayName, String initials) {}

    record ThreadInfo(Long id, String subject) {}

    record Run(Sender sender, ThreadInfo thread, boolean outbound, List<MessageDto> messages) {

        static Run from(SenderBubbleRun run) {
            Sender sender = run.sender() == null ? null
                    : new Sender(run.sender().email(), run.sender().displayName(), run.sender().initials());
            ThreadInfo thread = run.thread() == null ? null
                    : new ThreadInfo(run.thread().threadId(), run.thread().subject());
            return new Run(sender, thread, run.outbound(),
                    run.messages().stream().map(MessageDto::from).toList());
        }
    }

    record MessageDto(Long id, String bodyHtml, Instant sentAt, boolean outbound,
                      Long replyToMessageId, List<AttachmentDto> attachments) {

        static MessageDto from(BubbleMessage m) {
            return new MessageDto(m.messageId(), m.bodyHtml(), utc(m.sentAt()), m.outbound(),
                    m.replyToMessageId(),
                    m.attachments().stream().map(AttachmentDto::from).toList());
        }
    }

    record AttachmentDto(Long id, String filename, String mimeType, Long sizeBytes) {

        static AttachmentDto from(Attachment a) {
            return new AttachmentDto(a.getId(), a.getFilename(), a.getMimeType(), a.getSizeBytes());
        }
    }

    // Timestamps are stored as UTC LocalDateTime; the wire format is an ISO-8601 instant.
    private static Instant utc(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }
}
