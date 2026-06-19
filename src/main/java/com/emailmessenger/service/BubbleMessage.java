package com.emailmessenger.service;

import com.emailmessenger.domain.Attachment;
import java.time.LocalDateTime;
import java.util.List;

public record BubbleMessage(
    Long messageId,
    String bodyHtml,
    LocalDateTime sentAt,
    List<Attachment> attachments,
    boolean outbound,
    // DB id of the message this one is a reply to, when that message is also in
    // the same conversation view — lets the UI link to it instead of quoting it.
    Long replyToMessageId
) {}
