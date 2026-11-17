package com.emailmessenger.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

import java.util.Set;

/**
 * Plain-text rendering of a sanitized bubble body for clients that do not
 * render HTML (docs/API.md §5.6 {@code bodyText}). Jsoup's {@code text()}
 * flattens everything onto one line, which loses the paragraph breaks people
 * actually wrote.
 */
public final class HtmlText {

    private static final Set<String> BLOCKS = Set.of(
            "p", "div", "li", "tr", "blockquote", "pre", "h1", "h2", "h3", "h4", "h5", "h6",
            "ul", "ol", "table", "hr");

    private HtmlText() {}

    public static String toPlainText(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        Document doc = Jsoup.parseBodyFragment(html);
        StringBuilder out = new StringBuilder();
        NodeTraversor.traverse(new NodeVisitor() {
            @Override
            public void head(Node node, int depth) {
                if (node instanceof TextNode text) {
                    boolean inPre = text.parent() instanceof Element parent
                            && parent.closest("pre") != null;
                    out.append(inPre ? text.getWholeText() : collapse(text.getWholeText()));
                } else if (node instanceof Element el) {
                    if (el.normalName().equals("br")) {
                        out.append('\n');
                    } else if (BLOCKS.contains(el.normalName())) {
                        out.append('\n');
                    }
                }
            }

            @Override
            public void tail(Node node, int depth) {
                if (node instanceof Element el && BLOCKS.contains(el.normalName())) {
                    out.append('\n');
                }
            }
        }, doc.body());
        return tidy(out.toString());
    }

    private static String collapse(String text) {
        return text.replace(' ', ' ').replaceAll("[ \\t\\r\\n]+", " ");
    }

    private static String tidy(String text) {
        String[] lines = text.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        boolean blank = false;
        for (String line : lines) {
            String trimmed = line.strip();
            if (trimmed.isEmpty()) {
                blank = !sb.isEmpty();
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(blank ? "\n\n" : "\n");
            }
            sb.append(trimmed);
            blank = false;
        }
        return sb.toString();
    }
}
