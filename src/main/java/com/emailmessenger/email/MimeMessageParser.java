package com.emailmessenger.email;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

class MimeMessageParser {

    ParsedEmail parse(MimeMessage msg) throws MessagingException, IOException {
        String messageId = msg.getMessageID() != null ? msg.getMessageID().trim() : null;
        String inReplyTo = singleHeader(msg, "In-Reply-To");
        List<String> references = parseReferences(msg);
        String subject = msg.getSubject() != null ? msg.getSubject() : "(no subject)";

        InternetAddress from = firstFrom(msg);
        String fromEmail = from != null ? from.getAddress().toLowerCase() : "unknown@unknown";
        String fromName = from != null ? from.getPersonal() : null;

        var to = extractAddresses(msg, Message.RecipientType.TO);
        var cc = extractAddresses(msg, Message.RecipientType.CC);
        var bcc = extractAddresses(msg, Message.RecipientType.BCC);

        var textParts = new ArrayList<String[]>();
        var attachments = new ArrayList<ParsedEmail.AttachmentEntry>();
        walkParts(msg, textParts, attachments);

        String bodyPlain = textParts.stream()
                .filter(p -> "text/plain".equals(p[0])).findFirst().map(p -> p[1]).orElse(null);
        String bodyHtml = textParts.stream()
                .filter(p -> "text/html".equals(p[0])).findFirst().map(p -> p[1]).orElse(null);

        return new ParsedEmail(messageId, inReplyTo, references, subject,
                fromEmail, fromName, to, cc, bcc, bodyPlain, bodyHtml, attachments,
                toLocalDateTime(msg.getSentDate()));
    }

    private String singleHeader(MimeMessage msg, String name) throws MessagingException {
        String[] vals = msg.getHeader(name);
        return (vals != null && vals.length > 0) ? vals[0].trim() : null;
    }

    private List<String> parseReferences(MimeMessage msg) throws MessagingException {
        String raw = singleHeader(msg, "References");
        if (raw == null || raw.isBlank()) return Collections.emptyList();
        return Arrays.stream(raw.split("\\s+"))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private InternetAddress firstFrom(MimeMessage msg) throws MessagingException {
        Address[] from = msg.getFrom();
        if (from == null || from.length == 0) return null;
        return from[0] instanceof InternetAddress ia ? ia : null;
    }

    private List<ParsedEmail.AddressEntry> extractAddresses(MimeMessage msg, Message.RecipientType type)
            throws MessagingException {
        Address[] addrs = msg.getRecipients(type);
        if (addrs == null) return Collections.emptyList();
        var result = new ArrayList<ParsedEmail.AddressEntry>();
        for (Address a : addrs) {
            if (a instanceof InternetAddress ia && ia.getAddress() != null) {
                result.add(new ParsedEmail.AddressEntry(ia.getAddress().toLowerCase(), ia.getPersonal()));
            }
        }
        return result;
    }

    private void walkParts(Part part, List<String[]> textParts,
            List<ParsedEmail.AttachmentEntry> attachments) throws MessagingException, IOException {
        boolean isAttachment = Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition());
        String filename = part.getFileName();

        if (!isAttachment && part.isMimeType("text/plain")) {
            textParts.add(new String[]{"text/plain", (String) part.getContent()});
        } else if (!isAttachment && part.isMimeType("text/html")) {
            textParts.add(new String[]{"text/html", (String) part.getContent()});
        } else if (part.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) part.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                walkParts(mp.getBodyPart(i), textParts, attachments);
            }
        } else if (filename != null || isAttachment) {
            String mimeType = part.getContentType().split(";")[0].trim().toLowerCase();
            Long size = part.getSize() > 0 ? (long) part.getSize() : null;
            attachments.add(new ParsedEmail.AttachmentEntry(
                    filename != null ? filename : "attachment", mimeType, size));
        }
    }

    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) return LocalDateTime.now();
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
