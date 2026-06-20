package com.emailmessenger.service;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
class IMTransformService {

    String stripQuotes(String body) {
        if (body == null) return "";

        String[] lines = body.split("\n", -1);
        List<String> kept = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.strip();

            // "On <date>, <name> wrote:" attribution — Gmail, Apple Mail, Outlook
            if (isAttributionStart(lines, i)) {
                break;
            }

            // Outlook "-----Original Message-----" divider
            if (line.matches("-{3,}\\s*[Oo]riginal [Mm]essage\\s*-{3,}")) {
                break;
            }

            // Outlook reply header: "From: ..." followed by Sent:/To:/Subject:
            if (isOutlookHeader(lines, i)) {
                break;
            }

            // Signature delimiter ("-- ") and mobile signatures end the message.
            if (trimmed.equals("--")
                    || trimmed.regionMatches(true, 0, "Sent from my ", 0, 13)) {
                break;
            }

            // > quoted lines
            if (!line.startsWith(">")) {
                kept.add(line);
            }
        }

        String result = String.join("\n", kept);
        // Collapse 3+ consecutive blank lines to 2
        result = result.replaceAll("\n{3,}", "\n\n");
        return result.strip();
    }

    /**
     * The signature trailing a plain-text body — the lines after a standalone
     * "--" delimiter or a "Sent from my ..." line — or "" if there is none.
     * Mirrors the boundaries {@link #stripQuotes} cuts at, so a signature that
     * sits below quoted history is not picked up.
     */
    String extractSignature(String body) {
        if (body == null) return "";

        String[] lines = body.split("\n", -1);
        int start = -1;
        for (int i = 0; i < lines.length; i++) {
            if (isQuoteBoundary(lines, i)) {
                return ""; // quoted history starts before any signature
            }
            String trimmed = lines[i].strip();
            if (trimmed.equals("--")
                    || trimmed.regionMatches(true, 0, "Sent from my ", 0, 13)) {
                start = i;
                break;
            }
        }
        if (start < 0) return "";

        List<String> kept = new ArrayList<>();
        for (int i = start; i < lines.length; i++) {
            // Drop the bare "--" delimiter itself, but keep "Sent from my ...".
            if (i == start && lines[i].strip().equals("--")) continue;
            if (isQuoteBoundary(lines, i) || lines[i].startsWith(">")) break;
            kept.add(lines[i]);
        }
        return String.join("\n", kept).replaceAll("\n{3,}", "\n\n").strip();
    }

    private boolean isQuoteBoundary(String[] lines, int i) {
        return isAttributionStart(lines, i)
                || lines[i].matches("-{3,}\\s*[Oo]riginal [Mm]essage\\s*-{3,}")
                || isOutlookHeader(lines, i);
    }

    // An Outlook-style quoted header: a "From:" line with Sent:/To:/Subject:
    // within the next few lines.
    private boolean isOutlookHeader(String[] lines, int i) {
        String line = lines[i].strip();
        if (line.length() < 5 || !line.regionMatches(true, 0, "From:", 0, 5)) {
            return false;
        }
        for (int j = i; j < Math.min(i + 5, lines.length); j++) {
            String l = lines[j].strip();
            if (l.regionMatches(true, 0, "Sent:", 0, 5)
                    || l.regionMatches(true, 0, "To:", 0, 3)
                    || l.regionMatches(true, 0, "Subject:", 0, 8)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAttributionStart(String[] lines, int i) {
        String line = lines[i];
        if (!line.startsWith("On ")) return false;
        if (line.endsWith("wrote:")) return true;
        // Attribution may wrap over 2-3 lines
        StringBuilder joined = new StringBuilder(line);
        for (int j = i + 1; j < Math.min(i + 4, lines.length); j++) {
            joined.append(" ").append(lines[j].trim());
            if (lines[j].trim().endsWith("wrote:")) return true;
        }
        return false;
    }

    String renderMarkdown(String text) {
        if (text == null || text.isBlank()) return "";

        String html = escapeHtml(text);

        // Bold: **text** or __text__
        html = html.replaceAll("\\*\\*([^*\n]+?)\\*\\*", "<strong>$1</strong>");
        html = html.replaceAll("__([^_\n]+?)__", "<strong>$1</strong>");

        // Italic: *text* or _text_  (applied after bold so ** isn't confused)
        html = html.replaceAll("\\*([^*\n]+?)\\*", "<em>$1</em>");
        html = html.replaceAll("_([^_\n]+?)_", "<em>$1</em>");

        // Inline code: `code`
        html = html.replaceAll("`([^`\n]+?)`", "<code>$1</code>");

        // URLs → anchor links
        html = html.replaceAll("(https?://[\\w\\-./?=&%#+:@!~,;]+)", "<a href=\"$1\">$1</a>");

        // Paragraphs: blank lines become <p> boundaries; single newlines become <br>
        String[] paragraphs = html.split("\n\n+");
        StringBuilder sb = new StringBuilder();
        for (String para : paragraphs) {
            String trimmed = para.strip();
            if (!trimmed.isEmpty()) {
                sb.append("<p>").append(trimmed.replace("\n", "<br>")).append("</p>\n");
            }
        }
        return sb.toString().strip();
    }

    private String escapeHtml(String text) {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }
}
