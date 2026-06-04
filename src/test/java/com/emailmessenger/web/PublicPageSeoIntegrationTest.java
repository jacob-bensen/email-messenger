package com.emailmessenger.web;

import com.emailmessenger.billing.BillingService;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.service.ReplyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that every public page renders the SEO fragment with a unique
 * title, description, canonical URL, OG, and Twitter-card meta tags, so
 * a Google crawl / Twitter unfurl / Slack preview will surface the right
 * content. Boots the full Spring context (templates included).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestPropertySource(properties = "marketing.base-url=https://test.mailaim.app")
class PublicPageSeoIntegrationTest {

    @Autowired MockMvc mockMvc;

    // Stub the side-paths so the dev profile boots without a real SMTP/Stripe.
    @MockBean ReplyService replyService;
    @MockBean EmailThreadRepository threadRepository;
    @MockBean BillingService billingService;

    @Test
    void landingPageRendersUniqueSeoTags() throws Exception {
        String body = render("/");
        assertThat(body).contains("<title>MailIM — Your inbox, as a chat</title>");
        assertThat(body).contains("<meta name=\"description\" content=\"MailIM turns email threads into a modern instant-message conversation");
        assertThat(body).contains("<link rel=\"canonical\" href=\"https://test.mailaim.app/\"");
        assertThat(body).contains("<meta property=\"og:url\" content=\"https://test.mailaim.app/\"");
        assertThat(body).contains("<meta property=\"og:title\" content=\"MailIM — Your inbox, as a chat\"");
        assertThat(body).contains("<meta property=\"og:image\" content=\"https://test.mailaim.app/images/og-card.png\"");
        assertThat(body).contains("<meta name=\"twitter:card\" content=\"summary_large_image\"");
        assertThat(body).contains("<meta name=\"twitter:title\" content=\"MailIM — Your inbox, as a chat\"");
    }

    @Test
    void pricingPageRendersUniqueSeoTags() throws Exception {
        String body = render("/pricing");
        assertThat(body).contains("<title>Pricing — MailIM</title>");
        assertThat(body).contains("<meta name=\"description\" content=\"Simple, transparent pricing for MailIM");
        assertThat(body).contains("<link rel=\"canonical\" href=\"https://test.mailaim.app/pricing\"");
        assertThat(body).contains("<meta property=\"og:url\" content=\"https://test.mailaim.app/pricing\"");
        assertThat(body).contains("<meta property=\"og:title\" content=\"Pricing — MailIM\"");
        assertThat(body).contains("<meta name=\"twitter:card\" content=\"summary_large_image\"");
    }

    @Test
    void loginPageRendersUniqueSeoTags() throws Exception {
        String body = render("/login");
        assertThat(body).contains("<title>Sign in — MailIM</title>");
        assertThat(body).contains("<meta name=\"description\" content=\"Sign in to MailIM");
        assertThat(body).contains("<link rel=\"canonical\" href=\"https://test.mailaim.app/login\"");
        assertThat(body).contains("<meta property=\"og:url\" content=\"https://test.mailaim.app/login\"");
        assertThat(body).contains("<meta property=\"og:title\" content=\"Sign in — MailIM\"");
    }

    @Test
    void registerPageRendersUniqueSeoTags() throws Exception {
        String body = render("/register");
        assertThat(body).contains("<title>Create your account — MailIM</title>");
        assertThat(body).contains("<meta name=\"description\" content=\"Create a free MailIM account");
        assertThat(body).contains("<link rel=\"canonical\" href=\"https://test.mailaim.app/register\"");
        assertThat(body).contains("<meta property=\"og:url\" content=\"https://test.mailaim.app/register\"");
        assertThat(body).contains("<meta property=\"og:title\" content=\"Create your account — MailIM\"");
    }

    @Test
    void publicPagesHaveDistinctTitles() throws Exception {
        String landingTitle = extractTitle(render("/"));
        String pricingTitle = extractTitle(render("/pricing"));
        String loginTitle = extractTitle(render("/login"));
        String registerTitle = extractTitle(render("/register"));
        String demoTitle = extractTitle(render("/demo"));
        assertThat(landingTitle).isNotEqualTo(pricingTitle);
        assertThat(landingTitle).isNotEqualTo(loginTitle);
        assertThat(landingTitle).isNotEqualTo(registerTitle);
        assertThat(landingTitle).isNotEqualTo(demoTitle);
        assertThat(pricingTitle).isNotEqualTo(loginTitle);
        assertThat(pricingTitle).isNotEqualTo(registerTitle);
        assertThat(pricingTitle).isNotEqualTo(demoTitle);
        assertThat(loginTitle).isNotEqualTo(registerTitle);
        assertThat(loginTitle).isNotEqualTo(demoTitle);
        assertThat(registerTitle).isNotEqualTo(demoTitle);
    }

    @Test
    void demoPageRendersConversationWithSeoTagsAndStartFreeCta() throws Exception {
        String body = render("/demo");
        assertThat(body).contains("<title>Live demo — MailIM</title>");
        assertThat(body).contains("<meta name=\"description\" content=\"See an email thread rendered as an IM-style chat");
        assertThat(body).contains("<link rel=\"canonical\" href=\"https://test.mailaim.app/demo\"");
        assertThat(body).contains("<meta property=\"og:url\" content=\"https://test.mailaim.app/demo\"");
        // Curated demo content must actually render.
        assertThat(body).contains("Launch checklist");
        assertThat(body).contains("Alex Lee");
        assertThat(body).contains("Sam Patel");
        assertThat(body).contains("Maya Chen");
        // Demo banner + footer must surface the signup CTA so cold visitors convert.
        assertThat(body).contains("class=\"demo-banner\"");
        assertThat(body).contains("/register?utm_source=demo");
        // Reply form is for real threads only — it must NOT render in demo mode.
        assertThat(body).doesNotContain("name=\"body\"");
        assertThat(body).doesNotContain("/threads/null/reply");
    }

    @Test
    void robotsAndSitemapAreServedWithCanonicalBaseUrl() throws Exception {
        String robots = mockMvc.perform(get("/robots.txt"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(robots).contains("Sitemap: https://test.mailaim.app/sitemap.xml");

        String sitemap = mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(sitemap).contains("<loc>https://test.mailaim.app/</loc>");
        assertThat(sitemap).contains("<loc>https://test.mailaim.app/pricing</loc>");
        assertThat(sitemap).contains("<loc>https://test.mailaim.app/demo</loc>");
    }

    private String render(String path) throws Exception {
        MvcResult result = mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    private String extractTitle(String html) {
        int open = html.indexOf("<title>");
        int close = html.indexOf("</title>");
        return open < 0 || close < 0 ? "" : html.substring(open + 7, close);
    }
}
