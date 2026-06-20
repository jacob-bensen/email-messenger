package com.emailmessenger.web;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;

/**
 * PWA install surface: web app manifest + brand-mark icons in the sizes
 * Chrome / Android / iOS Safari need before they will offer "Install" or
 * "Add to Home Screen". Icons are generated server-side from the brand
 * color so a redeploy that changes branding is one-line — no asset
 * pipeline.
 */
@Controller
class PwaController {

    static final String APP_NAME = "ConexusMail";
    static final String APP_SHORT_NAME = "ConexusMail";
    static final String START_URL = "/chats";
    static final String SCOPE = "/";
    static final String THEME_COLOR = "#2f855a";
    static final String BACKGROUND_COLOR = "#0f1512";

    /**
     * Static-shell assets pre-cached by the service worker on install.
     * Listed once so the JS body, the test assertions, and the cache-bust
     * version hash all derive from the same source of truth.
     */
    static final List<String> SHELL_ASSETS = List.of(
            "/offline",
            "/css/main.css",
            "/icons/icon-192.png",
            "/manifest.webmanifest"
    );

    /**
     * Service-worker JS source. The {@code __CACHE_VERSION__} placeholder is
     * replaced on every render with a deterministic hash of this template +
     * the shell-asset list, so any code change here or asset addition busts
     * stale client caches on the next install (browsers re-fetch /sw.js on
     * navigation and treat any byte diff as "new worker → run install").
     */
    private static final String SW_TEMPLATE = """
            'use strict';
            const CACHE_VERSION = '__CACHE_VERSION__';
            const SHELL_ASSETS = __SHELL_ASSETS__;

            self.addEventListener('install', (event) => {
              event.waitUntil(
                caches.open(CACHE_VERSION)
                  .then((cache) => cache.addAll(SHELL_ASSETS))
                  .then(() => self.skipWaiting())
              );
            });

            self.addEventListener('activate', (event) => {
              event.waitUntil(
                caches.keys()
                  .then((keys) => Promise.all(
                    keys.filter((k) => k !== CACHE_VERSION).map((k) => caches.delete(k))
                  ))
                  .then(() => self.clients.claim())
              );
            });

            self.addEventListener('fetch', (event) => {
              const req = event.request;
              if (req.method !== 'GET') return;
              if (req.mode === 'navigate') {
                event.respondWith(
                  fetch(req).catch(() => caches.match('/offline'))
                );
                return;
              }
              const url = new URL(req.url);
              if (url.origin === self.location.origin && SHELL_ASSETS.indexOf(url.pathname) !== -1) {
                event.respondWith(
                  caches.match(req).then((cached) => cached || fetch(req))
                );
              }
            });
            """;

    private final String swBody = renderServiceWorker();
    private final String cacheVersion = extractCacheVersion(swBody);

    @GetMapping(value = "/manifest.webmanifest", produces = "application/manifest+json")
    ResponseEntity<String> manifest() {
        String body = "{\n"
                + "  \"name\": \"" + APP_NAME + " — Your inbox, as a chat\",\n"
                + "  \"short_name\": \"" + APP_SHORT_NAME + "\",\n"
                + "  \"description\": \"Email threads rendered as a modern IM-style chat — bubbles, avatars, day separators, dark mode.\",\n"
                + "  \"start_url\": \"" + START_URL + "\",\n"
                + "  \"scope\": \"" + SCOPE + "\",\n"
                + "  \"display\": \"standalone\",\n"
                + "  \"orientation\": \"portrait\",\n"
                + "  \"lang\": \"en\",\n"
                + "  \"dir\": \"ltr\",\n"
                + "  \"theme_color\": \"" + THEME_COLOR + "\",\n"
                + "  \"background_color\": \"" + BACKGROUND_COLOR + "\",\n"
                + "  \"icons\": [\n"
                + "    {\n"
                + "      \"src\": \"/icons/icon-192.png\",\n"
                + "      \"sizes\": \"192x192\",\n"
                + "      \"type\": \"image/png\",\n"
                + "      \"purpose\": \"any\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"src\": \"/icons/icon-512.png\",\n"
                + "      \"sizes\": \"512x512\",\n"
                + "      \"type\": \"image/png\",\n"
                + "      \"purpose\": \"any\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"src\": \"/icons/icon-512-maskable.png\",\n"
                + "      \"sizes\": \"512x512\",\n"
                + "      \"type\": \"image/png\",\n"
                + "      \"purpose\": \"maskable\"\n"
                + "    }\n"
                + "  ]\n"
                + "}\n";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/manifest+json"))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .body(body);
    }

    @GetMapping(value = "/icons/icon-192.png", produces = MediaType.IMAGE_PNG_VALUE)
    ResponseEntity<byte[]> icon192() {
        return pngResponse(renderBrandIcon(192, false));
    }

    @GetMapping(value = "/icons/icon-512.png", produces = MediaType.IMAGE_PNG_VALUE)
    ResponseEntity<byte[]> icon512() {
        return pngResponse(renderBrandIcon(512, false));
    }

    @GetMapping(value = "/icons/icon-512-maskable.png", produces = MediaType.IMAGE_PNG_VALUE)
    ResponseEntity<byte[]> icon512Maskable() {
        return pngResponse(renderBrandIcon(512, true));
    }

    @GetMapping(value = "/apple-touch-icon.png", produces = MediaType.IMAGE_PNG_VALUE)
    ResponseEntity<byte[]> appleTouchIcon() {
        return pngResponse(renderBrandIcon(180, false));
    }

    @GetMapping(value = "/sw.js", produces = "application/javascript")
    ResponseEntity<String> serviceWorker() {
        // Per spec the SW script must never be served from cache or the
        // browser will keep activating the same worker forever.
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/javascript"))
                .cacheControl(CacheControl.noStore())
                .header("Service-Worker-Allowed", "/")
                .body(swBody);
    }

    @GetMapping(value = "/offline", produces = MediaType.TEXT_HTML_VALUE)
    ResponseEntity<String> offline() {
        String body = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
                  <meta name="robots" content="noindex">
                  <title>You're offline — ConexusMail</title>
                  <meta name="theme-color" content="#2f855a">
                  <link rel="manifest" href="/manifest.webmanifest">
                  <link rel="apple-touch-icon" href="/apple-touch-icon.png">
                  <link rel="icon" type="image/png" sizes="192x192" href="/icons/icon-192.png">
                  <link rel="stylesheet" href="/css/main.css">
                  <style>
                    body.offline-shell { display: flex; min-height: 100vh; margin: 0;
                      align-items: center; justify-content: center;
                      background: #0f1512; color: #f8fafc;
                      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
                      padding: env(safe-area-inset-top) env(safe-area-inset-right)
                              env(safe-area-inset-bottom) env(safe-area-inset-left); }
                    .offline-card { max-width: 28rem; text-align: center; padding: 2rem; }
                    .offline-mark { font-size: 2rem; font-weight: 700; color: #5fb98a;
                      letter-spacing: -0.02em; margin: 0 0 1rem; }
                    .offline-title { font-size: 1.5rem; margin: 0 0 0.75rem; }
                    .offline-sub { color: #cbd5e1; line-height: 1.5; margin: 0 0 1.5rem; }
                    .offline-btn { display: inline-block; padding: 0.75rem 1.5rem;
                      background: #2f855a; color: #fff; border: none; border-radius: 0.5rem;
                      font: inherit; font-weight: 600; cursor: pointer; text-decoration: none; }
                    .offline-btn:hover { background: #276749; }
                  </style>
                </head>
                <body class="offline-shell">
                  <main class="offline-card">
                    <p class="offline-mark">ConexusMail</p>
                    <h1 class="offline-title">You're offline</h1>
                    <p class="offline-sub">
                      Your inbox is waiting. We'll reconnect to ConexusMail as soon
                      as your network is back.
                    </p>
                    <button class="offline-btn" onclick="location.reload()" type="button">
                      Try again
                    </button>
                  </main>
                </body>
                </html>
                """;
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
                .body(body);
    }

    String cacheVersion() {
        return cacheVersion;
    }

    private static String renderServiceWorker() {
        String assetsJsArray = SHELL_ASSETS.stream()
                .map(p -> "'" + p + "'")
                .reduce((a, b) -> a + ", " + b)
                .map(s -> "[" + s + "]")
                .orElse("[]");
        String withAssets = SW_TEMPLATE.replace("__SHELL_ASSETS__", assetsJsArray);
        // Fold the cached stylesheet's content into the version hash so editing
        // main.css ships a new cache key — otherwise the cache-first fetch handler
        // serves the stale stylesheet forever (template changes still show because
        // navigations always hit the network; static shell assets do not).
        String version = hash12(withAssets + cssFingerprint());
        return withAssets.replace("__CACHE_VERSION__", "conexusmail-shell-" + version);
    }

    private static String cssFingerprint() {
        try (var in = new org.springframework.core.io.ClassPathResource("static/css/main.css")
                .getInputStream()) {
            return hash12(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            // Missing stylesheet on the classpath shouldn't break SW generation;
            // the worst case is the pre-fix behaviour (version ignores CSS).
            return "";
        }
    }

    private static String extractCacheVersion(String body) {
        int start = body.indexOf("'conexusmail-shell-");
        if (start < 0) {
            return "conexusmail-shell";
        }
        int end = body.indexOf('\'', start + 1);
        return body.substring(start + 1, end);
    }

    private static String hash12(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(12);
            for (int i = 0; i < 6; i++) {
                hex.append(String.format("%02x", digest[i] & 0xff));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 unavailable", e);
        }
    }

    /**
     * Draws a square brand-mark icon: brand-blue background, white
     * speech-bubble glyph, "MI" wordmark. The maskable variant inflates
     * the background to the full bounds and shrinks the glyph into the
     * inner 80% safe zone so Android's adaptive-icon crop doesn't clip
     * the mark.
     */
    private BufferedImage renderBrandIcon(int size, boolean maskable) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Background. Maskable: full square so adaptive masks don't show transparent
            // corners. Standard: rounded square with brand-blue fill.
            g.setColor(Color.decode(THEME_COLOR));
            if (maskable) {
                g.fillRect(0, 0, size, size);
            } else {
                int radius = Math.round(size * 0.22f);
                g.fillRoundRect(0, 0, size, size, radius, radius);
            }

            // Inner safe zone for the glyph. Maskable spec wants the
            // recognisable content inside the centre 80% so a circle/squircle
            // crop doesn't eat it.
            int inset = maskable ? Math.round(size * 0.18f) : Math.round(size * 0.12f);
            int innerSize = size - inset * 2;

            // Speech-bubble glyph: white rounded rectangle with a tail in
            // the lower-left so the IM-bubble metaphor reads at favicon size.
            g.setColor(Color.WHITE);
            int bubbleW = innerSize;
            int bubbleH = Math.round(innerSize * 0.62f);
            int bubbleX = inset;
            int bubbleY = inset + Math.round(innerSize * 0.06f);
            int bubbleR = Math.round(bubbleW * 0.22f);
            g.fillRoundRect(bubbleX, bubbleY, bubbleW, bubbleH, bubbleR, bubbleR);

            int tailW = Math.round(innerSize * 0.14f);
            int tailH = Math.round(innerSize * 0.14f);
            int[] xs = {
                    bubbleX + Math.round(bubbleW * 0.18f),
                    bubbleX + Math.round(bubbleW * 0.18f) + tailW,
                    bubbleX + Math.round(bubbleW * 0.18f)
            };
            int[] ys = {
                    bubbleY + bubbleH - 2,
                    bubbleY + bubbleH - 2,
                    bubbleY + bubbleH + tailH
            };
            g.fillPolygon(xs, ys, 3);

            // "MI" wordmark inside the bubble.
            g.setColor(Color.decode(THEME_COLOR));
            int fontSize = Math.round(bubbleH * 0.58f);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fontSize));
            FontMetrics fm = g.getFontMetrics();
            String label = "MI";
            int tw = fm.stringWidth(label);
            int textX = bubbleX + (bubbleW - tw) / 2;
            int textY = bubbleY + (bubbleH + fm.getAscent() - fm.getDescent()) / 2;
            g.drawString(label, textX, textY);

            g.setComposite(AlphaComposite.SrcOver);
        } finally {
            g.dispose();
        }
        return img;
    }

    private ResponseEntity<byte[]> pngResponse(BufferedImage img) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(img, "png", baos);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .body(baos.toByteArray());
    }
}
