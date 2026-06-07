# MASTER_ACTIONS

Items genuinely waiting on a human — credentials, accounts, legal, or
asset delivery — that the agent cannot complete in code. Group by area.

## Infrastructure

- [ ] Provision a PostgreSQL 16 database (Supabase / Neon / Railway /
      self-hosted) and provide `DB_URL`, `DB_USER`, `DB_PASS`.
- [ ] Choose a hosting platform (Render / Railway / Fly / Heroku) and
      configure deploy with `SPRING_PROFILES_ACTIVE=prod`.
- [ ] Register a production domain (e.g. `mailaim.app`) and point DNS at
      the hosting platform; provide the chosen domain so URLs/redirects
      can be hard-wired.

## Stripe (blocks PLAN milestones 2–4)

- [ ] Create a Stripe account; provide `STRIPE_PUBLIC_KEY` and
      `STRIPE_SECRET_KEY` (test mode is fine to start).
- [ ] Create Stripe Products + Prices for Personal $9/mo, Team $29/mo,
      Enterprise $99/mo (plus matching annual prices at 2 months free).
      Hand over the four price IDs.
- [ ] Configure the Stripe webhook endpoint at `/billing/webhook` after
      deploy and provide `STRIPE_WEBHOOK_SECRET`.
- [ ] In the Stripe Dashboard, enable the Customer Billing Portal and
      configure the customer-facing features (update payment method,
      cancel, switch plans between Personal/Team); set the portal default
      return URL to `https://<prod-domain>/threads` and supply
      `BILLING_PORTAL_RETURN_URL` to the deploy.

## Mailbox encryption (blocks safe prod deploy)

- [ ] Generate `MAILBOX_ENCRYPTION_PASSWORD` (a strong random string) and
      `MAILBOX_ENCRYPTION_SALT` (hex-encoded 8 bytes,
      e.g. `openssl rand -hex 8`) and supply both to the deploy. Without
      these the app boots with a known dev-fallback key (warned in logs)
      that leaves every stored IMAP password trivially decryptable.

## OAuth & third-party APIs

- [ ] Create a Google Cloud project, enable OAuth 2.0, register the
      production redirect URI, and provide `GOOGLE_CLIENT_ID` /
      `GOOGLE_CLIENT_SECRET`.
- [ ] Pick a transactional email provider (Postmark / Resend / SendGrid),
      verify the sending domain, and provide the API key.

## Legal (Stripe & GDPR/OAuth gate)

- [ ] Review the shipped Privacy / Terms / Refund boilerplate at
      `/privacy`, `/terms`, `/refund` (sources in
      `src/main/resources/legal/`). Either accept as-is for the Stripe
      live-mode application, or replace per page via env override:
      `MARKETING_LEGAL_PRIVACY`, `MARKETING_LEGAL_TERMS`,
      `MARKETING_LEGAL_REFUND` (each takes a Spring resource locator,
      e.g. `file:/etc/mailim/privacy.html` or `https://termly.io/...`).

## Plan review

- [ ] [PLAN-REVIEW] EPIC-10 Mobile / PWA is code-complete (manifest +
      icons, service worker + offline shell, install banner with iOS
      fallback, mobile-tuned threads + conversation view with
      `viewport-fit=cover`, safe-area insets, ≥44px tap targets,
      sticky reply form via `100dvh`, sticky day-separator headers).
      Adopt one of: **annual-billing surfacing** (toggle annual vs
      monthly on `/pricing`, pass `?billing=annual` through Stripe
      Checkout for the 2-months-free SKU — direct ARPU lift),
      **Gmail OAuth mailbox connection** (replaces IMAP-password
      friction with one-click signin — biggest activation gain,
      depends on the Google OAuth credentials master action below),
      or **first-paying-customer attribution** (per-customer
      utm_source → Subscription, dashboard at `/admin/revenue`).

## Launch / marketing

- [ ] Record a 60–90 second Loom or YouTube demo of an email thread
      transforming into the chat view; supply the file or URL for the
      landing page hero embed.
- [ ] Draft Product Hunt launch assets (tagline, description, gallery)
      and schedule launch for a Tuesday 12:01 AM PT after MVP is live.
- [ ] Create the @MailIM Twitter/X account and a public Slack or Discord
      community; provide invite link for the footer.
