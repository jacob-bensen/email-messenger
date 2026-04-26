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

- [ ] [LEGAL] Add a Privacy Policy page (required for GDPR/CCPA and for any OAuth or payment flow).
      Can use a generator like Termly or Iubenda initially.
- [ ] [LEGAL] Add Terms of Service page.
- [ ] [LEGAL] Add Refund Policy page (required by Stripe and consumer protection law).
- [ ] [LEGAL] Add Cookie banner / consent if targeting EU users (GDPR cookie consent).
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
- [ ] [MARKETING] Set up a "waitlist" landing page (simple HTML or Carrd) before the MVP is done
      to capture early interest and validate demand.
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
