package com.emailmessenger.web;

import com.emailmessenger.billing.BillingService;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.service.ReplyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
@TestPropertySource(properties = "marketing.base-url=https://test.conexusmail.com")
class PublicPageSeoIntegrationTest {

    @Autowired MockMvc mockMvc;

    // Stub the side-paths so the dev profile boots without a real SMTP/Stripe.
    @MockitoBean ReplyService replyService;
    @MockitoBean EmailThreadRepository threadRepository;
    @MockitoBean BillingService billingService;

    @Test
    void landingPageRendersUniqueSeoTags() throws Exception {
        String body = render("/");
        assertThat(body).contains("<title>ConexusMail — Your inbox, as a chat</title>");
        assertThat(body).contains("<meta name=\"description\" content=\"ConexusMail turns email threads into a modern instant-message conversation");
        assertThat(body).contains("<link rel=\"canonical\" href=\"https://test.conexusmail.com/\"");
        assertThat(body).contains("<meta property=\"og:url\" content=\"https://test.conexusmail.com/\"");
        assertThat(body).contains("<meta property=\"og:title\" content=\"ConexusMail — Your inbox, as a chat\"");
        assertThat(body).contains("<meta property=\"og:image\" content=\"https://test.conexusmail.com/images/og-card.png\"");
        assertThat(body).contains("<meta name=\"twitter:card\" content=\"summary_large_image\"");
        assertThat(body).contains("<meta name=\"twitter:title\" content=\"ConexusMail — Your inbox, as a chat\"");
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
        assertThat(body).contains("<title>Pricing — ConexusMail</title>");
        assertThat(body).contains("<meta name=\"description\" content=\"ConexusMail is free");
        assertThat(body).contains("<link rel=\"canonical\" href=\"https://test.conexusmail.com/pricing\"");
        assertThat(body).contains("<meta property=\"og:url\" content=\"https://test.conexusmail.com/pricing\"");
        assertThat(body).contains("<meta property=\"og:title\" content=\"Pricing — ConexusMail\"");
        assertThat(body).contains("<meta name=\"twitter:card\" content=\"summary_large_image\"");
    }

    @Test
    void loginPageRendersUniqueSeoTags() throws Exception {
        String body = render("/login");
        assertThat(body).contains("<title>Sign in — ConexusMail</title>");
        assertThat(body).contains("<meta name=\"description\" content=\"Sign in to ConexusMail");
        assertThat(body).contains("<link rel=\"canonical\" href=\"https://test.conexusmail.com/login\"");
        assertThat(body).contains("<meta property=\"og:url\" content=\"https://test.conexusmail.com/login\"");
        assertThat(body).contains("<meta property=\"og:title\" content=\"Sign in — ConexusMail\"");
    }

    @Test
    void registerPageRendersUniqueSeoTags() throws Exception {
        String body = render("/register");
        assertThat(body).contains("<title>Create your account — ConexusMail</title>");
        assertThat(body).contains("<meta name=\"description\" content=\"Create a free ConexusMail account");
        assertThat(body).contains("<link rel=\"canonical\" href=\"https://test.conexusmail.com/register\"");
        assertThat(body).contains("<meta property=\"og:url\" content=\"https://test.conexusmail.com/register\"");
        assertThat(body).contains("<meta property=\"og:title\" content=\"Create your account — ConexusMail\"");
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
        assertThat(body).contains("<title>Live demo — ConexusMail</title>");
        assertThat(body).contains("<meta name=\"description\" content=\"See an email thread rendered as an IM-style chat");
        assertThat(body).contains("<link rel=\"canonical\" href=\"https://test.conexusmail.com/demo\"");
        assertThat(body).contains("<meta property=\"og:url\" content=\"https://test.conexusmail.com/demo\"");
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
        assertThat(robots).contains("Sitemap: https://test.conexusmail.com/sitemap.xml");

        String sitemap = mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(sitemap).contains("<loc>https://test.conexusmail.com/</loc>");
        assertThat(sitemap).contains("<loc>https://test.conexusmail.com/pricing</loc>");
        assertThat(sitemap).contains("<loc>https://test.conexusmail.com/demo</loc>");
        // Legal pages must show up in the sitemap so Stripe / search crawlers
        // can verify the published-policy URLs exist.
        assertThat(sitemap).contains("<loc>https://test.conexusmail.com/privacy</loc>");
        assertThat(sitemap).contains("<loc>https://test.conexusmail.com/terms</loc>");
        assertThat(sitemap).contains("<loc>https://test.conexusmail.com/refund</loc>");
    }

    @Test
    void privacyPageRendersDefaultBoilerplateContentAndSeoTags() throws Exception {
        String body = render("/privacy");
        assertThat(body).contains("<title>Privacy Policy — ConexusMail</title>");
        assertThat(body).contains("<link rel=\"canonical\" href=\"https://test.conexusmail.com/privacy\"");
        // Shipped boilerplate must render through the legal-page Thymeleaf wrapper.
        assertThat(body).contains("What we collect");
        assertThat(body).contains("privacy@conexusmail.com");
    }

    @Test
    void termsPageRendersDefaultBoilerplateContentAndSeoTags() throws Exception {
        String body = render("/terms");
        assertThat(body).contains("<title>Terms of Service — ConexusMail</title>");
        assertThat(body).contains("<link rel=\"canonical\" href=\"https://test.conexusmail.com/terms\"");
        assertThat(body).contains("Acceptable use");
        assertThat(body).contains("legal@conexusmail.com");
    }

    @Test
    void refundPageRendersDefaultBoilerplateContentAndSeoTags() throws Exception {
        String body = render("/refund");
        assertThat(body).contains("<title>Refund Policy — ConexusMail</title>");
        assertThat(body).contains("<link rel=\"canonical\" href=\"https://test.conexusmail.com/refund\"");
        assertThat(body).contains("Annual subscriptions");
        assertThat(body).contains("billing@conexusmail.com");
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
        assertThat(body).contains("conexusmail-cookie-consent-v1");
    }

    @Test
    void landingPageAdvertisesPwaManifestAndThemeColor() throws Exception {
        // PWA "Install" prompt requires a <link rel=manifest>, a
        // theme-color meta, and an apple-touch-icon for iOS. They must
        // all render on the landing page so a first-touch visitor on
        // Chrome / iOS Safari sees an installable site.
        String body = render("/");
        assertThat(body).contains("<link rel=\"manifest\" href=\"/manifest.webmanifest\"");
        assertThat(body).contains("<meta name=\"theme-color\" content=\"#2f855a\"");
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
        assertThat(body).contains("\"start_url\": \"/chats\"");
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
        assertThat(offline).contains("You're offline").contains("ConexusMail");
    }

    @Test
    void landingPageRegistersServiceWorkerOnLoad() throws Exception {
        // Without the registration <script>, the install prompt fires but
        // the offline cache never seeds — and an installed PWA opened in
        // airplane mode shows a browser error instead of ConexusMail's screen.
        String body = render("/");
        assertThat(body).contains("serviceWorker.register('/sw.js'");
    }

    @Test
    void landingPageRendersInstallBannerWithIosFallbackAndPersistentDismiss() throws Exception {
        // PWA install: Android/Chromium gets a one-click "Install" banner
        // wired to the stashed `beforeinstallprompt` event. iOS Safari
        // doesn't fire that event, so we feature-detect the UA and show
        // an "Add to Home Screen" walkthrough instead. Either dismissal
        // persists to localStorage so a returning visitor isn't pestered.
        String body = render("/");
        assertThat(body).contains("id=\"install-banner\"");
        assertThat(body).contains("id=\"install-banner-default\"");
        assertThat(body).contains("id=\"install-banner-ios\"");
        assertThat(body).contains("id=\"install-banner-cta\"");
        assertThat(body).contains("id=\"install-banner-dismiss\"");
        assertThat(body).contains("Install ConexusMail");
        assertThat(body).contains("Add ConexusMail to your home screen");
        // JS wiring: beforeinstallprompt is the Chromium signal; the iOS
        // UA test is what triggers the share-sheet walkthrough.
        assertThat(body).contains("beforeinstallprompt");
        assertThat(body).contains("/iPad|iPhone|iPod/");
        // Already-installed detection: standalone display-mode (Chrome) +
        // navigator.standalone (legacy iOS). The banner has to hide in
        // the installed app or it nags forever.
        assertThat(body).contains("display-mode: standalone");
        assertThat(body).contains("navigator.standalone");
        // Dismiss persistence: a versioned localStorage key so we can
        // future-version the banner copy without re-pestering everyone.
        assertThat(body).contains("conexusmail-install-dismiss-v1");
    }

    @Test
    void mainStylesheetCarriesMobileTuningForInstalledPwa() throws Exception {
        // EPIC-10 milestone 4: an installed ConexusMail on a 375px iPhone needs
        // ≥44px tap targets, safe-area insets so chrome doesn't slide under
        // the notch / home bar, a sticky reply form via 100dvh, and sticky
        // day-separator headers as the user scrolls a long thread. These
        // are CSS-only changes — assert the rules persist in the shipped
        // /css/main.css so a future refactor that drops them fails CI.
        String css = mockMvc.perform(get("/css/main.css"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(css).contains("100dvh");
        assertThat(css).contains("env(safe-area-inset-bottom");
        assertThat(css).contains("env(safe-area-inset-top");
        assertThat(css).contains("min-height: 44px");
        assertThat(css).contains(".day-separator");
        assertThat(css).contains("position: sticky");
    }

    @Test
    void pricingPageFramesAnnualBillingAsTwoMonthsFreeWithCashAmount() throws Exception {
        // EPIC-11 milestone 2: annual savings copy + value framing.
        // The toggle badge has to read "2 months free" (not "Save 16%") —
        // SaaS pricing comparison sites describe annual discounts in
        // months-free terms, which is a stronger anchor for the Free →
        // Personal decision. Each paid plan card also has to surface the
        // cash-amount the user will actually be charged today, so they
        // don't click through expecting $7 and see $84 on the Stripe page.
        String body = render("/pricing");
        assertThat(body).contains("2 months free");
        assertThat(body).doesNotContain("Save 16%");
        // The annual-cash sub-line is rendered with the cash amount in a
        // data attribute so the toggle JS can fill it in only when the
        // Annual tab is active — but the markup itself must already ship
        // the dollar amount so the value frame survives JS being blocked.
        assertThat(body).contains("data-annual-cash=\"Billed annually as $84\"");
        assertThat(body).contains("data-annual-cash=\"Billed annually as $288\"");
        assertThat(body).contains("data-annual-cash=\"Billed annually as $996\"");
        // The toggle JS keys on this class to swap the cash line on/off.
        assertThat(body).contains("class=\"plan-annual-cash\"");
    }

    @Test
    void registerPageAcknowledgesAnnualChoiceWhenBillingParamIsAnnual() throws Exception {
        // EPIC-11 milestone 2: the auth card has to acknowledge the annual
        // choice picked on /pricing so a user doesn't lose context across
        // the page transition — without it, the signup form gives the user
        // no cue that their pricing pick was actually carried through.
        String body = render("/register?plan=personal&billing=annual");
        assertThat(body).contains("class=\"auth-billing-badge\"");
        assertThat(body).contains("Annual billing");
        assertThat(body).contains("2 months free");
        // The hidden form fields must round-trip both plan + billing into
        // the POST so the redirect into Stripe Checkout uses the annual
        // price ID.
        assertThat(body).contains("name=\"plan\"").contains("value=\"personal\"");
        assertThat(body).contains("name=\"billing\"").contains("value=\"annual\"");
    }

    @Test
    void registerPageOmitsAnnualBadgeWhenBillingParamAbsent() throws Exception {
        // Free signups and monthly signups must NOT show the annual badge —
        // it would mis-frame the price the user is about to commit to.
        String defaultBody = render("/register");
        assertThat(defaultBody).doesNotContain("class=\"auth-billing-badge\"");
        String monthlyBody = render("/register?plan=personal&billing=monthly");
        assertThat(monthlyBody).doesNotContain("class=\"auth-billing-badge\"");
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
