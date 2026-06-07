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
    void landingFallsBackToChatBubbleMockWhenNoDemoVideoConfigured() throws Exception {
        // The dev profile leaves marketing.landing.video.* empty by default,
        // so the static chat-bubble mock must still render and the video
        // embed wrapper must be absent — a fresh deploy without a video URL
        // shouldn't break the hero.
        String body = render("/");
        assertThat(body).contains("class=\"landing-screenshot\"");
        assertThat(body).contains("class=\"screenshot-mock\"");
        assertThat(body).doesNotContain("class=\"landing-video\"");
        assertThat(body).doesNotContain("data-embed-url");
        assertThat(body).doesNotContain("youtube-nocookie.com");
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
        // Legal pages must show up in the sitemap so Stripe / search crawlers
        // can verify the published-policy URLs exist.
        assertThat(sitemap).contains("<loc>https://test.mailaim.app/privacy</loc>");
        assertThat(sitemap).contains("<loc>https://test.mailaim.app/terms</loc>");
        assertThat(sitemap).contains("<loc>https://test.mailaim.app/refund</loc>");
    }

    @Test
    void privacyPageRendersDefaultBoilerplateContentAndSeoTags() throws Exception {
        String body = render("/privacy");
        assertThat(body).contains("<title>Privacy Policy — MailIM</title>");
        assertThat(body).contains("<link rel=\"canonical\" href=\"https://test.mailaim.app/privacy\"");
        // Shipped boilerplate must render through the legal-page Thymeleaf wrapper.
        assertThat(body).contains("What we collect");
        assertThat(body).contains("privacy@mailaim.app");
    }

    @Test
    void termsPageRendersDefaultBoilerplateContentAndSeoTags() throws Exception {
        String body = render("/terms");
        assertThat(body).contains("<title>Terms of Service — MailIM</title>");
        assertThat(body).contains("<link rel=\"canonical\" href=\"https://test.mailaim.app/terms\"");
        assertThat(body).contains("Acceptable use");
        assertThat(body).contains("legal@mailaim.app");
    }

    @Test
    void refundPageRendersDefaultBoilerplateContentAndSeoTags() throws Exception {
        String body = render("/refund");
        assertThat(body).contains("<title>Refund Policy — MailIM</title>");
        assertThat(body).contains("<link rel=\"canonical\" href=\"https://test.mailaim.app/refund\"");
        assertThat(body).contains("Annual subscriptions");
        assertThat(body).contains("billing@mailaim.app");
    }

    @Test
    void landingFooterLinksToAllThreeLegalPagesAndRendersCookieBanner() throws Exception {
        String body = render("/");
        // Footer must surface the legal links so a Stripe reviewer can find them.
        assertThat(body).contains("href=\"/privacy\"");
        assertThat(body).contains("href=\"/terms\"");
        assertThat(body).contains("href=\"/refund\"");
        // Cookie banner fragment is injected and the dismiss button is wired.
        assertThat(body).contains("id=\"cookie-banner\"");
        assertThat(body).contains("id=\"cookie-banner-dismiss\"");
        assertThat(body).contains("mailim-cookie-consent-v1");
    }

    @Test
    void landingPageAdvertisesPwaManifestAndThemeColor() throws Exception {
        // PWA "Install" prompt requires a <link rel=manifest>, a
        // theme-color meta, and an apple-touch-icon for iOS. They must
        // all render on the landing page so a first-touch visitor on
        // Chrome / iOS Safari sees an installable site.
        String body = render("/");
        assertThat(body).contains("<link rel=\"manifest\" href=\"/manifest.webmanifest\"");
        assertThat(body).contains("<meta name=\"theme-color\" content=\"#4f80ff\"");
        assertThat(body).contains("<link rel=\"apple-touch-icon\" href=\"/apple-touch-icon.png\"");
        assertThat(body).contains("apple-mobile-web-app-capable");
    }

    @Test
    void manifestServedAtCanonicalPathWithRequiredFields() throws Exception {
        // Full Spring filter chain: the manifest must be reachable from
        // an unauthenticated request (browsers fetch it before login).
        String body = mockMvc.perform(get("/manifest.webmanifest"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(body).contains("\"start_url\": \"/threads\"");
        assertThat(body).contains("\"display\": \"standalone\"");
        assertThat(body).contains("/icons/icon-192.png");
        assertThat(body).contains("/icons/icon-512.png");
    }

    @Test
    void pwaIconsAndAppleTouchIconAreReachableWithoutAuth() throws Exception {
        // Browsers fetch these from the install banner before the user
        // ever signs in — they have to be in the public allow-list.
        mockMvc.perform(get("/icons/icon-192.png")).andExpect(status().isOk());
        mockMvc.perform(get("/icons/icon-512.png")).andExpect(status().isOk());
        mockMvc.perform(get("/icons/icon-512-maskable.png")).andExpect(status().isOk());
        mockMvc.perform(get("/apple-touch-icon.png")).andExpect(status().isOk());
    }

    @Test
    void serviceWorkerAndOfflineShellAreReachableThroughSecurityFilterChain() throws Exception {
        // Browsers fetch /sw.js with no auth context, and the SW pre-caches
        // /offline at install time — both have to clear permitAll() before
        // anything downstream works.
        String sw = mockMvc.perform(get("/sw.js"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(sw).contains("CACHE_VERSION");

        String offline = mockMvc.perform(get("/offline"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(offline).contains("You're offline").contains("MailIM");
    }

    @Test
    void landingPageRegistersServiceWorkerOnLoad() throws Exception {
        // Without the registration <script>, the install prompt fires but
        // the offline cache never seeds — and an installed PWA opened in
        // airplane mode shows a browser error instead of MailIM's screen.
        String body = render("/");
        assertThat(body).contains("serviceWorker.register('/sw.js'");
    }

    @Test
    void registerPageLinksToTermsAndPrivacyInline() throws Exception {
        String body = render("/register");
        // The /register form must inline a "by creating an account you agree to…"
        // line — this is what makes the click-through binding to terms.
        assertThat(body).contains("auth-legal");
        assertThat(body).contains("href=\"/terms\"");
        assertThat(body).contains("href=\"/privacy\"");
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
