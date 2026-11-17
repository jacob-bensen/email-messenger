package com.emailmessenger.api;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeVisitor;

import java.util.Set;

/**
 * Plain text of a bubble's sanitized HTML for clients that do not render HTML
 * (docs/API.md §5.6 {@code bodyText}). Unlike jsoup's {@code text()}, keeps
 * line and paragraph breaks, since a chat bubble is mostly short paragraphs.
 */
final class HtmlText {

    private static final Set<String> BLOCKS = Set.of(
            "p", "div", "li", "ul", "ol", "blockquote", "pre", "table", "tr",
            "h1", "h2", "h3", "h4", "h5", "h6", "hr");

    private HtmlText() {}

    static String of(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        Jsoup.parseBodyFragment(html).body().traverse(new NodeVisitor() {
            @Override
            public void head(Node node, int depth) {
                if (node instanceof TextNode text) {
                    out.append(text.text());
                } else if (node instanceof Element el) {
                    if (el.normalName().equals("br")) {
                        out.append('\n');
                    } else if (BLOCKS.contains(el.normalName())) {
                        out.append("\n\n");
                    }
                }
            }

            @Override
            public void tail(Node node, int depth) {
                if (node instanceof Element el && BLOCKS.contains(el.normalName())) {
                    out.append("\n\n");
                }
            }
        });
        return out.toString()
                .replaceAll("[ \\t\\x0B\\f\\r]*\\n[ \\t\\x0B\\f\\r]*", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .replace(' ', ' ')
                .strip();
    }
}
