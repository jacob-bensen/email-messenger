package com.emailmessenger.auth;

import java.util.Set;

/**
 * Allowed appearance options and cookie handling shared by the appearance
 * settings page (which writes the cookies) and {@code NavModelAdvice} (which
 * reads them into {@code <html>} attributes). Theme/accent live in cookies, not
 * the database — they're per-device display preferences, applied server-side so
 * the page never flashes the wrong palette.
 */
public final class AppearanceSettings {

    public static final String THEME_COOKIE = "theme";
    public static final String ACCENT_COOKIE = "accent";

    public static final String DEFAULT_THEME = "system";
    public static final String DEFAULT_ACCENT = "green";

    public static final Set<String> THEMES = Set.of("system", "light", "dark");
    public static final Set<String> ACCENTS = Set.of("green", "blue", "purple", "rose");

    private AppearanceSettings() {}

    /** Normalizes a raw theme value to an allowed one, falling back to "system". */
    public static String theme(String raw) {
        return raw != null && THEMES.contains(raw) ? raw : DEFAULT_THEME;
    }

    /** Normalizes a raw accent value to an allowed one, falling back to "green". */
    public static String accent(String raw) {
        return raw != null && ACCENTS.contains(raw) ? raw : DEFAULT_ACCENT;
    }

    /** {@code data-theme} attribute value, or {@code null} for "system" (no override). */
    public static String themeAttribute(String rawCookie) {
        String theme = theme(rawCookie);
        return theme.equals(DEFAULT_THEME) ? null : theme;
    }

    /** {@code data-accent} attribute value, or {@code null} for the default green. */
    public static String accentAttribute(String rawCookie) {
        String accent = accent(rawCookie);
        return accent.equals(DEFAULT_ACCENT) ? null : accent;
    }
}
