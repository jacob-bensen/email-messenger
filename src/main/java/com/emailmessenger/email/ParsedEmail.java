package com.emailmessenger.email;

import java.time.LocalDateTime;
import java.util.List;

record ParsedEmail(
        String messageId,
        String inReplyTo,
        List<String> references,
        String subject,
        String fromEmail,
        String fromName,
        List<AddressEntry> toRecipients,
        List<AddressEntry> ccRecipients,
        List<AddressEntry> bccRecipients,
        String bodyPlain,
        String bodyHtml,
        List<AttachmentEntry> attachments,
        LocalDateTime sentAt
) {
    record AddressEntry(String email, String name) {}
    record AttachmentEntry(String filename, String mimeType, Long sizeBytes) {}
}
