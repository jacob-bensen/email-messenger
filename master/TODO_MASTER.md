# TODO for Master (Human Actions Required)

Items here require action outside the codebase — credentials, deployments,
third-party accounts, marketing, or legal steps.

---

## Infrastructure / Credentials

- [ ] [DEPLOY] Create a PostgreSQL 16 database (Supabase, Railway, Neon, or self-hosted).
      Set the connection details as environment variables: DB_URL, DB_USER, DB_PASS.
- [ ] [DEPLOY] Choose a hosting platform (Heroku, Render, Railway, or VPS) and configure
      the Spring Boot app to run there. Set SPRING_PROFILES_ACTIVE=prod.
- [ ] [DEPLOY] Register a domain name for the SaaS (e.g. mailaim.app or simliar) and
      point it at the hosting platform.

## Billing

- [ ] [BILLING] Create a Stripe account at stripe.com.
      Retrieve publishable key and secret key; set as STRIPE_PUBLIC_KEY and STRIPE_SECRET_KEY env vars.
- [ ] [BILLING] Create three Stripe products and prices matching the plan table in APP_SPEC.md
      (Personal $9/mo, Team $29/mo, Enterprise $99/mo). Note the price IDs for config.
- [ ] [BILLING] Configure Stripe webhook endpoint (pointing to /billing/webhook) and set
      STRIPE_WEBHOOK_SECRET env var.

## Legal

- [ ] [LEGAL] Add a Privacy Policy page (required for GDPR/CCPA and for any OAuth or payment flow).
      Can use a generator like Termly or Iubenda initially.
- [ ] [LEGAL] Add Terms of Service page.
- [ ] [LEGAL] Add Refund Policy page (required by Stripe and consumer protection law).
- [ ] [LEGAL] Add Cookie banner / consent if targeting EU users (GDPR cookie consent).

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
