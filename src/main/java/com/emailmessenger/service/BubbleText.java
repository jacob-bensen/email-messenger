package com.emailmessenger.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

/**
 * Plain text of a bubble's sanitized HTML for clients that do not render HTML
 * (docs/API.md §5.6 {@code bodyText}). Unlike {@code Element.text()}, keeps
 * the line structure: {@code <br>} and block boundaries become newlines.
 */
public final class BubbleText {

    private BubbleText() {}

    public static String of(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        NodeTraversor.traverse(new NodeVisitor() {
            @Override
            public void head(Node node, int depth) {
                if (node instanceof TextNode text) {
                    String collapsed = text.text().replaceAll("\\s+", " ");
                    if (endsWithBreak(out) && collapsed.startsWith(" ")) {
                        collapsed = collapsed.substring(1);
                    }
                    out.append(collapsed);
                } else if (node instanceof Element el) {
                    if ("br".equals(el.normalName())) {
                        trimTrailingSpace(out);
                        out.append('\n');
                    } else if (el.isBlock()) {
                        breakLine(out);
                    }
                }
            }

            @Override
            public void tail(Node node, int depth) {
                if (node instanceof Element el && el.isBlock()) {
                    breakLine(out);
                }
            }
        }, Jsoup.parseBodyFragment(html).body());
        return out.toString()
                .replaceAll("[ \\t]+\\n", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .strip();
    }

    private static boolean endsWithBreak(StringBuilder out) {
        return out.isEmpty() || out.charAt(out.length() - 1) == '\n';
    }

    private static void trimTrailingSpace(StringBuilder out) {
        while (!out.isEmpty() && out.charAt(out.length() - 1) == ' ') {
            out.setLength(out.length() - 1);
        }
    }

    // A block boundary starts a new line; it never adds an empty one.
    private static void breakLine(StringBuilder out) {
        trimTrailingSpace(out);
        if (!out.isEmpty() && out.charAt(out.length() - 1) != '\n') {
            out.append('\n');
        }
    }
}
