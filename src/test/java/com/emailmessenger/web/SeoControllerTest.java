package com.emailmessenger.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SeoControllerTest {

    private MockMvc mockMvc(String baseUrl) {
        SiteProperties props = new SiteProperties();
        props.setBaseUrl(baseUrl);
        return MockMvcBuilders.standaloneSetup(new SeoController(props)).build();
    }

    @Test
    void robotsAdvertisesSitemapAndDisallowsPrivateRoutes() throws Exception {
        MvcResult result = mockMvc("https://mailaim.app/")
                .perform(get("/robots.txt"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/plain"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body)
                .contains("User-agent: *")
                .contains("Allow: /")
                .contains("Disallow: /threads")
                .contains("Disallow: /mailboxes")
                .contains("Disallow: /billing/")
                .contains("Disallow: /actuator/")
                .contains("Sitemap: https://mailaim.app/sitemap.xml");
    }

    @Test
    void sitemapListsEveryPublicMarketingUrl() throws Exception {
        MvcResult result = mockMvc("https://mailaim.app")
                .perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/xml"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body)
                .startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                .contains("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">")
                .contains("<loc>https://mailaim.app/</loc>")
                .contains("<loc>https://mailaim.app/pricing</loc>")
                .contains("<loc>https://mailaim.app/demo</loc>")
                .contains("<loc>https://mailaim.app/register</loc>")
                .contains("<loc>https://mailaim.app/login</loc>")
                .contains("</urlset>");
    }

    @Test
    void ogCardServesValidPngWithCorrectDimensions() throws Exception {
        MvcResult result = mockMvc("https://mailaim.app")
                .perform(get("/images/og-card.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andReturn();

        byte[] body = result.getResponse().getContentAsByteArray();
        assertThat(body).isNotEmpty();
        // PNG magic number: 89 50 4E 47 0D 0A 1A 0A
        assertThat(body[0]).isEqualTo((byte) 0x89);
        assertThat(body[1]).isEqualTo((byte) 0x50);
        assertThat(body[2]).isEqualTo((byte) 0x4E);
        assertThat(body[3]).isEqualTo((byte) 0x47);

        java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(
                new java.io.ByteArrayInputStream(body));
        assertThat(img).isNotNull();
        assertThat(img.getWidth()).isEqualTo(1200);
        assertThat(img.getHeight()).isEqualTo(630);
    }

    @Test
    void baseUrlTrailingSlashIsStrippedBeforeUseInRobotsAndSitemap() throws Exception {
        // SiteProperties#setBaseUrl strips trailing slashes so callers don't have to.
        MvcResult robots = mockMvc("https://mailaim.app///")
                .perform(get("/robots.txt"))
                .andReturn();
        assertThat(robots.getResponse().getContentAsString())
                .contains("Sitemap: https://mailaim.app/sitemap.xml")
                .doesNotContain("https://mailaim.app///");

        MvcResult sitemap = mockMvc("https://mailaim.app///")
                .perform(get("/sitemap.xml"))
                .andReturn();
        assertThat(sitemap.getResponse().getContentAsString())
                .contains("<loc>https://mailaim.app/</loc>")
                .contains("<loc>https://mailaim.app/pricing</loc>")
                .doesNotContain("https://mailaim.app///");
    }
}
