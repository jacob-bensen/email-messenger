package com.emailmessenger.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

import java.util.Set;

/**
 * Plain-text rendering of sanitized bubble HTML for clients that do not render
 * HTML (docs/API.md §5.6 {@code text}): paragraphs are separated by a blank
 * line, other blocks and {@code <br>} by a line break; runs of blank lines
 * collapse to one and the result is trimmed.
 */
public final class HtmlText {

    private static final Set<String> PARAGRAPHS = Set.of(
            "p", "ul", "ol", "blockquote", "pre", "table", "h1", "h2", "h3", "h4", "h5", "h6", "hr");
    private static final Set<String> LINES = Set.of("div", "li", "tr");

    private HtmlText() {}

    public static String toPlainText(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        NodeTraversor.traverse(new NodeVisitor() {
            @Override
            public void head(Node node, int depth) {
                if (node instanceof TextNode text) {
                    boolean pre = node.parent() instanceof Element parent && parent.normalName().equals("pre");
                    out.append(pre ? text.getWholeText() : text.text());
                } else if (node instanceof Element el) {
                    if (el.normalName().equals("br")) {
                        out.append('\n');
                    } else {
                        breakBefore(out, el);
                    }
                }
            }

            @Override
            public void tail(Node node, int depth) {
                if (node instanceof Element el && !el.normalName().equals("br")) {
                    breakBefore(out, el);
                }
            }
        }, Jsoup.parseBodyFragment(html).body());
        return tidy(out.toString());
    }

    /** Pads (never stacks) line breaks at a block boundary: adjacent blocks share one separator. */
    private static void breakBefore(StringBuilder out, Element el) {
        String name = el.normalName();
        int wanted = PARAGRAPHS.contains(name) ? 2 : LINES.contains(name) ? 1 : 0;
        int trailing = 0;
        while (trailing < out.length() && out.charAt(out.length() - 1 - trailing) == '\n') {
            trailing++;
        }
        out.append("\n".repeat(Math.max(0, wanted - trailing)));
    }

    private static String tidy(String text) {
        String[] lines = text.replace('\u00a0', ' ').split("\n", -1);
        StringBuilder out = new StringBuilder();
        boolean blank = false;
        for (String line : lines) {
            String trimmed = line.strip();
            if (trimmed.isEmpty()) {
                blank = !out.isEmpty();
                continue;
            }
            if (!out.isEmpty()) {
                out.append(blank ? "\n\n" : "\n");
            }
            out.append(trimmed);
            blank = false;
        }
        return out.toString();
    }
}
