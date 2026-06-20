package com.emailmessenger.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.safety.Safelist;

import java.util.Locale;

/**
 * Trims an HTML email body down to just the new content of that message:
 *
 * <ul>
 *   <li>removes quoted reply history — Outlook "From:/Sent:/Subject:" headers
 *       and the text under them, Gmail/Apple {@code blockquote}/{@code .gmail_quote},
 *       and "On ... wrote:" / "-----Original Message-----" attributions;</li>
 *   <li>removes signatures that sit in a standard container (Outlook
 *       {@code #Signature}, mobile signatures);</li>
 *   <li>drops empty / whitespace-only blocks and zero-width characters that
 *       bloat the bubble with blank space;</li>
 *   <li>then sanitizes with the same safe allowlist used for inbound HTML.</li>
 * </ul>
 *
 * The aim is a chat bubble showing what the person actually wrote this time,
 * not the entire thread re-quoted underneath.
 */
final class EmailBodyCleaner {

    private static final Safelist SAFELIST = Safelist.relaxed();

    // Containers email clients wrap signatures in. We lift these out rather
    // than just dropping them, so the sender chat can pin the signature once
    // beside the timeline instead of repeating it in every bubble.
    private static final String SIGNATURE_CONTAINERS =
            "#Signature, #ms-outlook-mobile-signature, #ms-outlook-android-signature, "
            + ".gmail_signature, [data-smartmail=gmail_signature]";

    /** The body with its signature split out; both are sanitized HTML. */
    record Cleaned(String body, String signature) {}

    /** The message body with quotes and signature removed. */
    String clean(String rawHtml) {
        return process(rawHtml).body();
    }

    /** The signature lifted out of {@code rawHtml}, sanitized, or "" if none. */
    String extractSignature(String rawHtml) {
        return process(rawHtml).signature();
    }

    private Cleaned process(String rawHtml) {
        if (rawHtml == null || rawHtml.isBlank()) {
            return new Cleaned("", "");
        }
        Document doc = Jsoup.parse(rawHtml);
        Element body = doc.body();

        // 1. Lift out signatures that sit in a known container, before anything
        //    else rearranges the tree.
        StringBuilder signature = new StringBuilder();
        for (Element sig : body.select(SIGNATURE_CONTAINERS)) {
            append(signature, sig.html());
            sig.remove();
        }

        // 2. Known quote-history containers.
        body.select("blockquote, .gmail_quote, .gmail_extra, .moz-cite-prefix, "
                + "#divRplyFwdMsg").remove();

        // 3. Outlook appends its signature + quoted history after this marker.
        Element append = body.getElementById("appendonsend");
        if (append != null) {
            truncateFrom(append);
        }

        // 4. Generic: cut from the first reply-attribution header onward, which
        //    catches the inline "From:/Sent:" chains Outlook leaves in the body.
        Element boundary = findReplyBoundary(body);
        if (boundary != null) {
            truncateFrom(boundary);
        }

        // 5. With the quoted history gone, a standalone "--" delimiter marks a
        //    plain-text signature: everything after it is the signature, not
        //    new content. Only consult it when no container signature was found.
        if (signature.length() == 0) {
            append(signature, cutDelimiterSignature(body));
        }

        // 6. Drop blocks that are now empty (whitespace/nbsp only, no media).
        pruneEmpty(body);

        // 7. Sanitize both halves, then tidy leftover whitespace artifacts.
        return new Cleaned(
                tidy(Jsoup.clean(body.html(), SAFELIST)),
                tidy(Jsoup.clean(signature.toString(), SAFELIST)));
    }

    /**
     * Finds a leaf block whose only text is the RFC 3676 "--" signature
     * delimiter and returns the HTML that follows it (the signature),
     * removing the delimiter and everything after it from {@code body}.
     */
    private static String cutDelimiterSignature(Element body) {
        Element delimiter = null;
        for (Element el : body.getAllElements()) {
            if (el == body || !el.children().isEmpty()) {
                continue;
            }
            String tag = el.tagName();
            if ((tag.equals("div") || tag.equals("p") || tag.equals("span"))
                    && normalize(el.text()).equals("--")) {
                delimiter = el;
                break;
            }
        }
        if (delimiter == null) {
            return "";
        }
        StringBuilder sig = new StringBuilder();
        collectFollowing(delimiter, sig);
        Element cur = delimiter.parent();
        while (cur != null && !cur.tagName().equals("body")) {
            collectFollowing(cur, sig);
            cur = cur.parent();
        }
        truncateFrom(delimiter);
        return sig.toString();
    }

    private static void collectFollowing(Node node, StringBuilder sb) {
        Node sib = node.nextSibling();
        while (sib != null) {
            if (sib instanceof Element e) {
                append(sb, e.outerHtml());
            } else if (sib instanceof TextNode t && !t.text().isBlank()) {
                append(sb, t.text());
            }
            sib = sib.nextSibling();
        }
    }

    private static void append(StringBuilder sb, String html) {
        if (html == null) {
            return;
        }
        String trimmed = html.strip();
        if (trimmed.isEmpty()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append('\n');
        }
        sb.append(trimmed);
    }

    private static Element findReplyBoundary(Element body) {
        for (Element el : body.getAllElements()) {
            if (el != body && isReplyHeader(el)) {
                return el;
            }
        }
        return null;
    }

    private static boolean isReplyHeader(Element el) {
        String tag = el.tagName();
        // Only block-level containers, so a stray inline span can't trip it.
        if (!(tag.equals("div") || tag.equals("p") || tag.equals("table") || tag.equals("blockquote"))) {
            return false;
        }
        String lower = normalize(el.text()).toLowerCase(Locale.ROOT);
        if (lower.isEmpty()) {
            return false;
        }
        if (lower.startsWith("from:") && (lower.contains("sent:") || lower.contains("subject:"))) {
            return true;
        }
        if (lower.startsWith("on ") && lower.contains("wrote:")) {
            return true;
        }
        return lower.contains("-----original message-----");
    }

    /** Removes {@code from} and everything that follows it in document order. */
    private static void truncateFrom(Element from) {
        removeFollowingSiblings(from);
        Element parent = from.parent();
        from.remove();
        // Walk up: drop each ancestor's following siblings but keep the ancestor
        // itself (it still holds the kept content that preceded the boundary).
        Element cur = parent;
        while (cur != null && !cur.tagName().equals("body")) {
            removeFollowingSiblings(cur);
            cur = cur.parent();
        }
    }

    private static void removeFollowingSiblings(Node node) {
        Node sib = node.nextSibling();
        while (sib != null) {
            Node next = sib.nextSibling();
            sib.remove();
            sib = next;
        }
    }

    private static void pruneEmpty(Element body) {
        for (Element el : body.select("p, div, span")) {
            if (el.select("img, a, hr, table").isEmpty() && normalize(el.text()).isEmpty()) {
                el.remove();
            }
        }
    }

    private static String normalize(String s) {
        // Collapse non-breaking space, BOM/ZWNBSP and zero-width space, then trim.
        return s.replaceAll("[\u00A0\uFEFF\u200B]", " ").trim();
    }

    private static String tidy(String html) {
        String out = html.replaceAll("[\uFEFF\u200B]", "");
        // Collapse the long runs of <br> some clients use for vertical spacing.
        out = out.replaceAll("(?i)(<br\\s*/?>\\s*){3,}", "<br><br>");
        return out.strip();
    }
}
