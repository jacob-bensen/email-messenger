package com.emailmessenger.web;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Static crawler / unfurler endpoints: robots.txt, sitemap.xml, and a
 * generated Open Graph card image. All built dynamically against
 * {@link SiteProperties#getBaseUrl()} so the canonical domain is the
 * single source of truth (set per-env via MARKETING_BASE_URL).
 */
@Controller
class SeoController {

    private static final String[] PUBLIC_PATHS = {
            "/", "/pricing", "/demo", "/register", "/login",
            "/privacy", "/terms", "/refund"
    };

    private final SiteProperties siteProperties;

    SeoController(SiteProperties siteProperties) {
        this.siteProperties = siteProperties;
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    ResponseEntity<String> robots() {
        String body = "User-agent: *\n"
                + "Allow: /\n"
                + "Disallow: /threads\n"
                + "Disallow: /mailboxes\n"
                + "Disallow: /billing/\n"
                + "Disallow: /actuator/\n"
                + "Disallow: /h2-console/\n"
                + "\n"
                + "Sitemap: " + siteProperties.getBaseUrl() + "/sitemap.xml\n";
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .body(body);
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    ResponseEntity<String> sitemap() {
        String today = LocalDate.now(ZoneOffset.UTC).toString();
        StringBuilder xml = new StringBuilder(512);
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        for (String path : PUBLIC_PATHS) {
            xml.append("  <url>\n");
            xml.append("    <loc>").append(siteProperties.getBaseUrl()).append(path).append("</loc>\n");
            xml.append("    <lastmod>").append(today).append("</lastmod>\n");
            xml.append("    <changefreq>weekly</changefreq>\n");
            xml.append("    <priority>").append("/".equals(path) ? "1.0" : "0.7").append("</priority>\n");
            xml.append("  </url>\n");
        }
        xml.append("</urlset>\n");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .body(xml.toString());
    }

    @GetMapping(value = "/images/og-card.png", produces = MediaType.IMAGE_PNG_VALUE)
    ResponseEntity<byte[]> ogCard() {
        int w = 1200;
        int h = 630;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(new Color(15, 23, 42));
            g.fillRect(0, 0, w, h);

            g.setColor(new Color(99, 102, 241));
            g.fillRoundRect(80, 140, 540, 110, 32, 32);
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 32));
            g.drawString("Can you ship pricing today?", 120, 205);

            g.setColor(new Color(31, 41, 55));
            g.fillRoundRect(640, 280, 480, 110, 32, 32);
            g.setColor(Color.WHITE);
            g.drawString("Already in review.", 680, 345);

            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 72));
            g.drawString("MailIM", 80, 510);
            g.setColor(new Color(148, 163, 184));
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 36));
            g.drawString("Your inbox, as a chat.", 80, 560);
        } finally {
            g.dispose();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(img, "png", baos);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
                .body(baos.toByteArray());
    }
}
