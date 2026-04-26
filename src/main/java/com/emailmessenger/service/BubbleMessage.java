package com.emailmessenger.service;

import com.emailmessenger.domain.Attachment;
import java.time.LocalDateTime;
import java.util.List;

record BubbleMessage(
    Long messageId,
    String bodyHtml,
    LocalDateTime sentAt,
    List<Attachment> attachments
) {}
