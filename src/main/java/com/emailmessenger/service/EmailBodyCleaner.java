package com.emailmessenger.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
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

    String clean(String rawHtml) {
        if (rawHtml == null || rawHtml.isBlank()) {
            return "";
        }
        Document doc = Jsoup.parse(rawHtml);
        Element body = doc.body();

        // 1. Known quote + signature containers.
        body.select("blockquote, .gmail_quote, .gmail_extra, .moz-cite-prefix, "
                + "#divRplyFwdMsg, #Signature, #ms-outlook-mobile-signature, "
                + "#ms-outlook-android-signature").remove();

        // 2. Outlook appends its signature + quoted history after this marker.
        Element append = body.getElementById("appendonsend");
        if (append != null) {
            truncateFrom(append);
        }

        // 3. Generic: cut from the first reply-attribution header onward, which
        //    catches the inline "From:/Sent:" chains Outlook leaves in the body.
        Element boundary = findReplyBoundary(body);
        if (boundary != null) {
            truncateFrom(boundary);
        }

        // 4. Drop blocks that are now empty (whitespace/nbsp only, no media).
        pruneEmpty(body);

        // 5. Sanitize, then tidy leftover whitespace artifacts.
        return tidy(Jsoup.clean(body.html(), SAFELIST));
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
