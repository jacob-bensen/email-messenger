package com.emailmessenger.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a sanitized signature into a {@link ContactCard}. The rules are
 * documented in docs/API.md §5.6 ({@code members[].signature}); clients show
 * the result as given and do not re-parse.
 */
public final class SignatureCards {

    static final int MAX_ITEMS = 5;
    private static final int MAX_FIELD_LENGTH = 80;

    // A run of digits with the separators people put in phone numbers. The
    // 7–15 digit check below rejects most dates and reference numbers.
    private static final Pattern PHONE = Pattern.compile(
            "(?<![\\w/.@])\\+?\\(?\\d[\\d ().\\-\\u00a0]{5,}\\d(?![\\w/@])");
    private static final Pattern ISO_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern URL = Pattern.compile(
            "(?i)\\b(?:https?://|www\\.)[^\\s<>\"']+");
    private static final Pattern EMAIL = Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");
    private static final Pattern SEGMENT_SPLIT = Pattern.compile("\\s*[|•·]\\s*");
    private static final Pattern AT_SPLIT = Pattern.compile("^(.+?)\\s+at\\s+(.+)$");
    private static final Pattern SIGN_OFF = Pattern.compile(
            "(?i)^(best|thanks|thank you|many thanks|cheers|regards|kind regards|best regards|"
                    + "warm regards|sincerely|yours|all the best|sent from my\\b.*|get outlook for\\b.*)"
                    + "[,.!]*$");
    private static final Pattern PHONE_LABEL = Pattern.compile(
            "(?i)^(tel|phone|mobile|mob|cell|office|direct|fax|t|m|p|o|d|f)\\b\\s*[:.]?\\s*");
    private static final Pattern LETTER = Pattern.compile("\\p{L}");

    private SignatureCards() {}

    /**
     * The card for {@code signatureHtml}, or null when it is blank. {@code name}
     * and {@code email} identify the member so their own name is not taken for
     * a title.
     */
    public static ContactCard parse(String signatureHtml, String name, String email) {
        String text = HtmlText.toPlainText(signatureHtml);
        if (text.isEmpty()) {
            return null;
        }
        Element body = Jsoup.parseBodyFragment(signatureHtml).body();

        Map<String, ContactCard.Phone> phones = new LinkedHashMap<>();
        for (Element a : body.select("a[href]")) {
            String href = a.attr("href").strip();
            if (href.regionMatches(true, 0, "tel:", 0, 4)) {
                addPhone(phones, a.text().isBlank() ? href.substring(4) : a.text().strip(), href.substring(4));
            }
        }
        Matcher pm = PHONE.matcher(text);
        while (pm.find()) {
            addPhone(phones, pm.group().strip(), pm.group());
        }

        Map<String, String> websites = new LinkedHashMap<>();
        for (Element a : body.select("a[href]")) {
            addWebsite(websites, a.attr("href").strip());
        }
        Matcher um = URL.matcher(text);
        while (um.find()) {
            addWebsite(websites, um.group());
        }

        List<String> rest = new ArrayList<>();
        for (String line : text.split("\n")) {
            for (String segment : SEGMENT_SPLIT.split(line.strip())) {
                if (isDescriptive(segment, name, email)) {
                    rest.add(segment.strip());
                }
            }
        }
        String title = null;
        String company = null;
        if (rest.size() == 1) {
            Matcher at = AT_SPLIT.matcher(rest.get(0));
            if (at.matches()) {
                title = at.group(1).strip();
                company = at.group(2).strip();
            } else {
                title = rest.get(0);
            }
        } else if (rest.size() > 1) {
            title = rest.get(0);
            company = rest.get(1);
        }
        return new ContactCard(text, title, company,
                phones.values().stream().limit(MAX_ITEMS).toList(),
                websites.values().stream().limit(MAX_ITEMS).toList());
    }

    private static void addPhone(Map<String, ContactCard.Phone> phones, String display, String raw) {
        if (ISO_DATE.matcher(raw).find()) {
            return;
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() < 7 || digits.length() > 15) {
            return;
        }
        String tel = (raw.strip().startsWith("+") ? "+" : "") + digits;
        phones.putIfAbsent(tel, new ContactCard.Phone(display, tel));
    }

    private static void addWebsite(Map<String, String> websites, String raw) {
        String url = raw.replaceAll("[).,;:!?]+$", "");
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.startsWith("www.")) {
            url = "https://" + url;
        } else if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return;
        }
        String key = url.toLowerCase(Locale.ROOT).replaceAll("/+$", "");
        if (key.length() <= "https://".length()) {
            return;
        }
        websites.putIfAbsent(key, url);
    }

    /** "Ada", "Lovelace" or "Ada Lovelace" for a member named "Ada Lovelace". */
    private static boolean isOwnName(String segment, String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        Set<String> nameWords = Set.of(name.strip().toLowerCase(Locale.ROOT).split("\\s+"));
        for (String word : segment.toLowerCase(Locale.ROOT).replaceAll("[,.]", " ").strip().split("\\s+")) {
            if (!nameWords.contains(word)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isDescriptive(String segment, String name, String email) {
        String s = segment.strip();
        if (s.isEmpty() || s.length() > MAX_FIELD_LENGTH || !LETTER.matcher(s).find()) {
            return false;
        }
        if (s.equals("--") || SIGN_OFF.matcher(s).matches()) {
            return false;
        }
        if (isOwnName(s, name) || (email != null && s.equalsIgnoreCase(email.strip()))) {
            return false;
        }
        if (EMAIL.matcher(s).find() || URL.matcher(s).find()) {
            return false;
        }
        String withoutLabel = PHONE_LABEL.matcher(s).replaceFirst("");
        Matcher pm = PHONE.matcher(s);
        while (pm.find()) {
            String digits = pm.group().replaceAll("\\D", "");
            if (digits.length() >= 7 && digits.length() <= 15) {
                return false;
            }
        }
        return !withoutLabel.isEmpty();
    }
}
