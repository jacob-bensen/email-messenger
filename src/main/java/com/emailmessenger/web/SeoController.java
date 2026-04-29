package com.emailmessenger.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Controller
class SeoController {

    private static final List<String> PUBLIC_PATHS = List.of(
            "/",
            "/demo",
            "/pricing",
            "/waitlist",
            "/privacy",
            "/terms",
            "/refund"
    );

    private final String baseUrl;

    SeoController(@Value("${app.base-url:https://mailaim.app}") String baseUrl) {
        this.baseUrl = stripTrailingSlash(baseUrl);
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    String robots() {
        return """
                User-agent: *
                Allow: /
                Disallow: /h2-console/
                Disallow: /threads
                Disallow: /threads/

                Sitemap: %s/sitemap.xml
                """.formatted(baseUrl);
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    String sitemap() {
        String today = LocalDate.now(ZoneOffset.UTC).toString();
        StringBuilder xml = new StringBuilder(512);
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        for (String path : PUBLIC_PATHS) {
            xml.append("  <url>\n");
            xml.append("    <loc>").append(baseUrl).append(path).append("</loc>\n");
            xml.append("    <lastmod>").append(today).append("</lastmod>\n");
            xml.append("    <changefreq>").append(changefreqFor(path)).append("</changefreq>\n");
            xml.append("    <priority>").append(priorityFor(path)).append("</priority>\n");
            xml.append("  </url>\n");
        }
        xml.append("</urlset>\n");
        return xml.toString();
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String changefreqFor(String path) {
        return switch (path) {
            case "/" -> "weekly";
            case "/pricing", "/demo", "/waitlist" -> "weekly";
            default -> "monthly";
        };
    }

    private static String priorityFor(String path) {
        return switch (path) {
            case "/" -> "1.0";
            case "/waitlist", "/pricing" -> "0.9";
            case "/demo" -> "0.8";
            default -> "0.5";
        };
    }
}
