package com.emailmessenger.api;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/** Plain-text rendering of already-sanitized bubble HTML, for clients that don't render HTML. */
final class ApiText {

    // Private-use code point: survives Jsoup's whitespace normalization and never occurs in real mail.
    private static final String BREAK = "\uE000";

    private ApiText() {}

    /** Text content with block and {@code <br>} boundaries kept as newlines; "" for null/blank. */
    static String plain(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        Document doc = Jsoup.parseBodyFragment(html);
        doc.select("br").after(BREAK);
        doc.select("p, div, li, blockquote, pre, tr, h1, h2, h3, h4, h5, h6").after(BREAK);
        String text = doc.body().text().replace(BREAK, "\n");
        return text.replaceAll("[ \\t]*\\n[ \\t]*", "\n").replaceAll("\\n{3,}", "\n\n").strip();
    }
}
