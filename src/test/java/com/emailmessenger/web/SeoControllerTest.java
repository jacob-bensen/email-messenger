package com.emailmessenger.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;

class SeoControllerTest {

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SeoController("https://mailaim.app"))
                .build();
    }

    @Test
    void robotsReturnsTextPlain() throws Exception {
        mockMvc.perform(get("/robots.txt"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", startsWith("text/plain")));
    }

    @Test
    void robotsAllowsAllUserAgentsAndReferencesSitemap() throws Exception {
        mockMvc.perform(get("/robots.txt"))
                .andExpect(content().string(containsString("User-agent: *")))
                .andExpect(content().string(containsString("Allow: /")))
                .andExpect(content().string(containsString("Sitemap: https://mailaim.app/sitemap.xml")));
    }

    @Test
    void robotsDisallowsAuthGatedAndDevPaths() throws Exception {
        mockMvc.perform(get("/robots.txt"))
                .andExpect(content().string(containsString("Disallow: /h2-console/")))
                .andExpect(content().string(containsString("Disallow: /threads")));
    }

    @Test
    void sitemapReturnsXmlContentType() throws Exception {
        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", startsWith("application/xml")));
    }

    @Test
    void sitemapHasUrlsetWrapperAndSchema() throws Exception {
        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(content().string(startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")))
                .andExpect(content().string(containsString("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">")))
                .andExpect(content().string(containsString("</urlset>")));
    }

    @Test
    void sitemapIncludesEveryPublicUrl() throws Exception {
        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(content().string(containsString("<loc>https://mailaim.app/</loc>")))
                .andExpect(content().string(containsString("<loc>https://mailaim.app/demo</loc>")))
                .andExpect(content().string(containsString("<loc>https://mailaim.app/pricing</loc>")))
                .andExpect(content().string(containsString("<loc>https://mailaim.app/waitlist</loc>")))
                .andExpect(content().string(containsString("<loc>https://mailaim.app/privacy</loc>")))
                .andExpect(content().string(containsString("<loc>https://mailaim.app/terms</loc>")))
                .andExpect(content().string(containsString("<loc>https://mailaim.app/refund</loc>")));
    }

    @Test
    void sitemapMarksLandingAsHighestPriority() throws Exception {
        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(content().string(containsString("<priority>1.0</priority>")));
    }

    @Test
    void sitemapUsesTodaysDateAsLastmod() throws Exception {
        String today = LocalDate.now(ZoneOffset.UTC).toString();
        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(content().string(containsString("<lastmod>" + today + "</lastmod>")));
    }

    @Test
    void baseUrlConfigStripsTrailingSlash() throws Exception {
        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new SeoController("https://example.test/"))
                .build();
        mvc.perform(get("/sitemap.xml"))
                .andExpect(content().string(containsString("<loc>https://example.test/demo</loc>")))
                .andExpect(content().string(containsString("<loc>https://example.test/</loc>")));
    }
}
