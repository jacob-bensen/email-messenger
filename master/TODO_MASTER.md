# TODO for Master (Human Actions Required)

Items here require action outside the codebase — credentials, deployments,
third-party accounts, marketing, or legal steps.

---

## Critical Security

- [x] [LIKELY DONE - verify] HTML email body XSS: jsoup 1.17.2 added to pom.xml;
      ConversationService.buildBodyHtml now calls Jsoup.clean(bodyHtml, Safelist.relaxed())
      before returning. Scripts, event handlers, and javascript: links stripped. 4 XSS tests added.
      Verify sanitization behavior in production with real-world HTML email bodies.

## Infrastructure / Credentials

- [ ] [DEPLOY] DEPLOYMENT UNBLOCKED: Docker Compose + CI are now ready. The app is deployable. Priority action: follow the steps below to get MailIM live. Once running, the waitlist URL can be shared publicly and revenue conversations can begin.
- [ ] [DEPLOY] Docker Compose is ready. Run `cp .env.example .env`, fill in real values (DB_PASS, MAIL_*, IMAP_*), then `docker compose up -d` to start the app + postgres. The app will be available on port 8080 (or PORT env var). First boot runs Flyway migrations automatically.
- [ ] [DEPLOY] GitHub Actions CI is configured in `.github/workflows/ci.yml`. To activate: push the repo to GitHub, ensure Java 21 and Maven cache are available on the runner. CI runs on every push and PR — no additional setup needed.
- [ ] [DEPLOY] Enable IMAP polling in production: set these env vars once the app is deployed:
      IMAP_POLLING_ENABLED=true, IMAP_HOST=imap.gmail.com (or your IMAP host), IMAP_PORT=993,
      IMAP_SSL=true, IMAP_USER=your-email@domain.com, IMAP_PASS=your-app-password,
      IMAP_FOLDER=INBOX, IMAP_POLLING_INTERVAL_MS=60000. For Gmail, generate an App Password
      at myaccount.google.com/apppasswords (requires 2FA enabled).
- [ ] [DEPLOY] Create a PostgreSQL 16 database (Supabase, Railway, Neon, or self-hosted).
      Set the connection details as environment variables: DB_URL, DB_USER, DB_PASS.
- [ ] [DEPLOY] Choose a hosting platform (Heroku, Render, Railway, or VPS) and configure
      the Spring Boot app to run there. Set SPRING_PROFILES_ACTIVE=prod.
- [ ] [DEPLOY] Register a domain name for the SaaS (e.g. mailaim.app or similar) and
      point it at the hosting platform.

## Billing

- [ ] [BILLING] Create a Stripe account at stripe.com.
      Retrieve publishable key and secret key; set as STRIPE_PUBLIC_KEY and STRIPE_SECRET_KEY env vars.
- [ ] [BILLING] Create three Stripe products and prices matching the plan table in APP_SPEC.md
      (Personal $9/mo, Team $29/mo, Enterprise $99/mo). Note the price IDs for config.
- [ ] [BILLING] Configure Stripe webhook endpoint (pointing to /billing/webhook) and set
      STRIPE_WEBHOOK_SECRET env var.

## Google OAuth (for GROWTH task: Google SSO)

- [ ] [CREDENTIAL] Create a Google Cloud project and enable the Google OAuth 2.0 API.
      Set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET env vars once code is ready.
      Required for the Google OAuth sign-in feature.

## Legal

- [ ] [LEGAL] Verify license compatibility of jakarta.mail (com.sun.mail:jakarta.mail:2.0.1). The
      CDDL 1.1 + GPL v2 with Classpath Exception license is generally considered safe for use in
      commercial applications (the Classpath Exception prevents GPL from propagating to the application
      code), but confirm with legal counsel before charging customers. Alternative: migrate to
      Eclipse Angus Mail (org.eclipse.angus:jakarta.mail) under EPL 2.0, which is unambiguously
      commercial-friendly.
- [ ] [LEGAL] Add a Privacy Policy page (required for GDPR/CCPA and for any OAuth or payment flow).
      Can use a generator like Termly or Iubenda initially.
- [ ] [LEGAL] Add Terms of Service page.
- [x] [LIKELY DONE - verify] Add Refund Policy page (required by Stripe and consumer protection law). /refund is live as a placeholder; replace with legally reviewed copy before accepting payments.
- [x] [LIKELY DONE - verify] Add Cookie banner / consent if targeting EU users (GDPR cookie consent). Banner is live on every public page (templates/fragments/cookie-banner.html + /js/cookie-banner.js + .cookie-banner CSS). 7 integration tests verify presence. Run #18. Verify in production with a real EU user before declaring fully shipped.
- [ ] [LEGAL] Review Google OAuth terms of service before launching SSO — ensure the app's
      privacy policy covers use of Google account data.

## Marketing

- [ ] [MARKETING] Create a Product Hunt launch page for MailIM. Draft a tagline, description,
      and screenshots once the UI is built. Schedule launch for a Tuesday 12:01 AM PT.
- [ ] [MARKETING] Submit to directories: AlternativeTo, Capterra, G2, SaaSHub — list MailIM
      as an alternative to email clients like Superhuman, HEY, Mimestream.
- [ ] [MARKETING] Write 3–5 SEO blog posts targeting: "email to chat", "email IM interface",
      "email like slack", "gmail alternative chat view". Publish on the app's /blog.
- [ ] [MARKETING] Create a Twitter/X account and LinkedIn page for MailIM. Post launch updates.
- [ ] [MARKETING] Post in communities: Indie Hackers, Hacker News (Show HN), Reddit r/productivity,
      r/selfhosted once a self-hosted Docker option is available.
- [x] [LIKELY DONE - verify] Set up a "waitlist" landing page: /waitlist is live with email capture, duplicate detection, social proof count, and success/already-joined states.
- [ ] [MARKETING] Create a Slack community or Discord server for early MailIM users — builds
      word-of-mouth and gives a feedback channel. Link from the app footer and README.
- [ ] [MARKETING] Once Slack/Discord webhook integration ships, post a targeted announcement in
      Slack communities (e.g. Indie Hackers Slack, remote work Slack groups) — contextually
      relevant to the integration and low-cost distribution.
- [ ] [MARKETING] Record a 60–90 second Loom or YouTube demo video showing an email thread
      transforming into the IM chat view in real time; embed on the landing page above the fold.
      This is the single highest-leverage conversion asset for skeptical visitors.
- [ ] [MARKETING] Build or commission a Chrome/Firefox browser extension that adds an
      "Open in MailIM" button inside Gmail threads — distributes the product as a side-effect of
      using Gmail; each install is a growth loop touchpoint. Requires extension review/approval.
- [ ] [MARKETING] List MailIM on AppSumo for a lifetime deal campaign: generates immediate lump-sum
      revenue, hundreds of reviews, and organic word-of-mouth. Best timed after the MVP is live and
      stable. Requires AppSumo partner application at appsumo.com/sell.
- [ ] [MARKETING] Set up an affiliate program via Rewardful or PartnerStack: offer 30% recurring
      commission to productivity bloggers, YouTubers, and Twitter/X influencers who drive signups.
      Requires account creation, affiliate agreement, and payout setup. HIGH income impact
      (commission-only customer acquisition = zero upfront ad spend).
- [ ] [MARKETING] Set up an NPS survey (Delighted, Typeform, or Survicate) triggered 30 days after
      signup: identify promoters (score 9-10) → ask for G2/Capterra reviews; identify detractors
      (score 0-6) → trigger personal outreach to prevent churn. MEDIUM income impact (retention
      + review velocity).
- [ ] [MARKETING] Once the templates are live and the app has a working UI, record a 15-second
      GIF/screen-recording showing an email thread transforming into chat bubbles and post it on
      Twitter/X, Indie Hackers, and LinkedIn. High-engagement visual content with strong viral
      potential in productivity communities. No code required — use LICEcap or Kap to capture.
- [ ] [MARKETING] Configure an Anthropic API key (ANTHROPIC_API_KEY env var) once the AI thread
      summary feature ships — needed to enable the Personal+ tier differentiator. Sign up at
      console.anthropic.com if no account exists.
- [ ] [MARKETING] Set up a transactional email provider (Postmark, SendGrid, or Resend) and
      configure three automated emails: (1) welcome email sent at signup, (2) "getting started"
      tip email at day 3, (3) upgrade prompt email at day 7 targeting active free-tier users.
      These 3 emails are the highest ROI re-engagement mechanism for early SaaS growth.
      Requires account creation and API key; dev will wire the triggers once user auth ships.
- [ ] [MARKETING] Now that IMAP polling is live, record a 15-second screen recording showing a real
      email arriving and appearing as a new chat bubble in MailIM — this is the single most persuasive
      demo asset for Product Hunt, landing page, and social media. Use LICEcap or Kap to capture;
      no code required. High viral potential in productivity communities.
- [ ] [INFRASTRUCTURE] Set up error monitoring (Sentry free tier) for the production deployment.
- [ ] [MARKETING] Set up Plausible Analytics (plausible.io, free tier) or Google Analytics 4 on the pricing page and thread list to measure conversion funnel. Without event tracking you cannot know whether visitors click "Start free trial" or which plan card they hover. Add the script snippet to all Thymeleaf templates once account is created.
- [ ] [MARKETING] Now that /demo is live, add the demo URL to all social profiles (Twitter/X bio, LinkedIn page, IndieHackers profile, README badges) and post a short announcement showing a screen recording of the demo page. /demo is the lowest-friction marketing asset — it lets anyone experience the product without signing up.
- [ ] [MARKETING] Once the /pricing page is live, update the project README, Indie Hackers profile, and any social media bios with the direct pricing URL. Link to it from the app's empty-state and thread list header.
- [ ] [MARKETING] Now that /waitlist is live, post the waitlist URL on Twitter/X, Indie Hackers, LinkedIn, and any relevant Slack/Discord communities (r/productivity, remote-work channels). Include the /demo link so visitors can try before joining. Even 10–20 signups provide social proof and validate demand before auth ships.
- [ ] [MARKETING] Set up a transactional email provider (Postmark, Resend, or SendGrid) so the waitlist confirmation email can fire automatically on signup. Configure MAIL_HOST / MAIL_USER / MAIL_PASS env vars accordingly. The waitlist confirmation email task in INTERNAL_TODO.md is blocked until this is done.
- [x] [LIKELY DONE - verify] Pricing page /privacy and /terms links: /privacy and /terms stub pages are now live, linked from pricing footer and FAQ. Verify content is sufficient before accepting EU users or payments.
- [ ] [INFRASTRUCTURE] Set up Sentry error monitoring for the production deployment.
- [ ] [MARKETING] Once gzip compression and Core Web Vitals improvements are deployed, run the site through Google PageSpeed Insights (pagespeed.web.dev) and submit to Google Search Console. Core Web Vitals score directly influences organic ranking — document the before/after LCP and CLS scores.
- [ ] [MARKETING] After adding JSON-LD FAQPage schema to /pricing, validate it at Google Rich Results Test (search.google.com/test/rich-results) and request indexing via Search Console. FAQ rich results increase CTR by 20-30% for target queries. IMAP polling runs on a background thread — uncaught exceptions are only visible in logs. Sentry will catch and alert on IMAP connection failures, import errors, and Spring exceptions. Sign up at sentry.io, add the sentry-spring-boot-starter dependency, set SENTRY_DSN env var.
- [ ] [MARKETING] Submit the /compare page (once built) to "best Superhuman alternatives" roundup posts on Medium, Reddit r/productivity, and IndieHackers. Contact the authors of existing "top email client" posts and suggest MailIM as an addition. Each backlink from a high-DA page is an organic growth multiplier.
- [ ] [MARKETING] Collect 3 real user quotes from waitlist signups (via a follow-up email: "In one sentence, what made you join the waitlist?"). Use verbatim quotes as testimonials on index.html and pricing.html. Real quotes outperform placeholder copy 5:1 on conversion.
- [ ] [MARKETING] Set APP_LAUNCH_DATE env var once a target launch date is decided. The dev-side waitlist position estimator (INTERNAL_TODO.md) uses this to show "Estimated access: [month]" on the waitlist success page — creates urgency and reduces churn from the pre-launch list.
- [ ] [MARKETING] Pick a transactional email provider this week (Postmark recommended for SaaS — flat $15/mo, simple API, good deliverability; Resend is the cheaper modern alternative). Three dev tasks (waitlist confirmation email, admin signup notification, future welcome series) are blocked on this single decision. Sign up, get an API key, set MAIL_HOST/MAIL_USER/MAIL_PASS env vars.
- [ ] [MARKETING] Once UTM-source capture is wired into /waitlist (see INTERNAL_TODO.md [GROWTH][S]), use unique utm_source URLs for every channel post (e.g. ?utm_source=twitter, ?utm_source=indiehackers, ?utm_source=reddit_productivity). This is how you'll know which channel actually drives signups vs. which just drives traffic. Free, no tooling needed.
- [ ] [MARKETING] Once the demo embed widget ships (see INTERNAL_TODO.md [GROWTH][S]), email the top 5 productivity-newsletter authors (Refind, Superorganizers, Make Time, etc.) offering a free embed of MailIM in their next "tools we love" issue. Embedded demos in newsletters convert at 3-8× a plain link.
- [ ] [MARKETING] Cookie consent banner is now live — this unblocks marketing to EU audiences. Re-evaluate distribution channels with EU reach (LinkedIn EU, Heise.de, Productivity-focused subreddits with strong EU traffic).
