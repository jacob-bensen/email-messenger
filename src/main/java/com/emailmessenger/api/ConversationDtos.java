package com.emailmessenger.api;

import com.emailmessenger.domain.Attachment;
import com.emailmessenger.service.BubbleMessage;
import com.emailmessenger.service.ChatConversation;
import com.emailmessenger.service.ChatMember;
import com.emailmessenger.service.ConversationListItem;
import com.emailmessenger.service.SenderBubbleRun;
import com.emailmessenger.service.SenderView;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Wire shapes for the conversations endpoints — a flat projection of the same
 * view models the Thymeleaf workspace renders. See docs/API.md.
 */
final class ConversationDtos {

    private ConversationDtos() {}

    record PageDto<T>(List<T> items, int page, int size, long totalItems, int totalPages, boolean hasNext) {

        static PageDto<SummaryDto> ofSummaries(Page<ConversationListItem> page) {
            return new PageDto<>(page.getContent().stream().map(SummaryDto::from).toList(),
                    page.getNumber(), page.getSize(), page.getTotalElements(),
                    page.getTotalPages(), page.hasNext());
        }
    }

    record SummaryDto(String key, String title, String initials, boolean group, int memberCount,
                      String preview, Instant lastActivityAt, boolean unread, long threadCount,
                      List<String> accounts, String senderEmail) {

        static SummaryDto from(ConversationListItem item) {
            return new SummaryDto(item.key(), item.title(), item.initials(), item.group(),
                    item.memberCount(), item.preview(), utc(item.lastActivity()), item.unread(),
                    item.threadCount(), item.accounts() == null ? List.of() : item.accounts(),
                    item.senderEmail());
        }
    }

    record DetailDto(String key, String title, String initials, boolean group, long threadCount,
                     List<MemberDto> members, List<MessageDto> messages) {

        static DetailDto from(ChatConversation chat) {
            List<MessageDto> messages = new ArrayList<>();
            for (SenderBubbleRun run : chat.runs()) {
                for (BubbleMessage bubble : run.messages()) {
                    messages.add(MessageDto.from(run, bubble));
                }
            }
            return new DetailDto(chat.key(), chat.title(), chat.initials(), chat.isGroup(),
                    chat.threadCount(), chat.members().stream().map(MemberDto::from).toList(), messages);
        }
    }

    record MemberDto(String email, String displayName, String initials, String label, String signatureText) {

        static MemberDto from(ChatMember member) {
            return new MemberDto(member.email(), member.displayName(), member.initials(),
                    member.label(), ApiText.plain(member.signatureHtml()));
        }
    }

    record SenderDto(String email, String displayName, String initials) {

        static SenderDto from(SenderView sender) {
            return new SenderDto(sender.email(), sender.displayName(), sender.initials());
        }
    }

    record MessageDto(Long id, Long threadId, String threadSubject, SenderDto sender, boolean outbound,
                      Instant sentAt, String bodyHtml, String bodyText, Long replyToMessageId,
                      List<AttachmentDto> attachments) {

        static MessageDto from(SenderBubbleRun run, BubbleMessage bubble) {
            return new MessageDto(bubble.messageId(), run.thread().threadId(), run.thread().subject(),
                    SenderDto.from(run.sender()), bubble.outbound(), utc(bubble.sentAt()),
                    bubble.bodyHtml(), ApiText.plain(bubble.bodyHtml()), bubble.replyToMessageId(),
                    bubble.attachments().stream().map(AttachmentDto::from).toList());
        }
    }

    record AttachmentDto(Long id, String filename, String mimeType, Long sizeBytes) {

        static AttachmentDto from(Attachment attachment) {
            return new AttachmentDto(attachment.getId(), attachment.getFilename(),
                    attachment.getMimeType(), attachment.getSizeBytes());
        }
    }

    // Timestamps are stored as UTC wall-clock LocalDateTimes; the web view makes
    // the same assumption in its *EpochMilli() accessors.
    private static Instant utc(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }
}
