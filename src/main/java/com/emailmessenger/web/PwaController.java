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
import java.time.Duration;

/**
 * PWA install surface: web app manifest + brand-mark icons in the sizes
 * Chrome / Android / iOS Safari need before they will offer "Install" or
 * "Add to Home Screen". Icons are generated server-side from the brand
 * color so a redeploy that changes branding is one-line — no asset
 * pipeline.
 */
@Controller
class PwaController {

    static final String APP_NAME = "MailIM";
    static final String APP_SHORT_NAME = "MailIM";
    static final String START_URL = "/threads";
    static final String SCOPE = "/";
    static final String THEME_COLOR = "#4f80ff";
    static final String BACKGROUND_COLOR = "#0f172a";

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
