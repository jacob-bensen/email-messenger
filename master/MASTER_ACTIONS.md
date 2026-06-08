# MASTER_ACTIONS

Items genuinely waiting on a human — credentials, accounts, legal, or
asset delivery — that the agent cannot complete in code. Group by area.

- [ ] [PLAN-REVIEW] EPIC-12 First-paying-customer attribution funnel is
      code-complete on all four milestones — `/admin/revenue` dashboard,
      Stripe pre-V17 billing-period backfill button, 30-day signup →
      trial → paid funnel pane, and the Monday 09:00 UTC operator email
      digest behind `ADMIN_WEEKLY_DIGEST_ENABLED`. Propose adopting
      **EPIC-13 Google OAuth signup** as the next Primary Objective —
      adds the "Continue with Google" path on `/login` and `/register`,
      auto-provisions the account on first OAuth callback, and lifts the
      `acquisition_source` from the OAuth state so the funnel dashboard
      starts attributing Google-sourced conversions. Unblocks the
      Google-credentials MASTER_ACTIONS line below and removes the
      "make another password" friction at the most leveraged conversion
      step (account-creation). Alternative: Mailbox onboarding wizard
      (first-run IMAP credential test + import-progress UI) or
      GreenMail-backed Testcontainers integration tests for the
      end-to-end IMAP → reply round-trip. Adopt one, update PLAN.md,
      then this line should be removed.

- [ ] Set `ADMIN_EMAILS` on the deploy to the comma-separated list of
      operator addresses that should see `/admin/revenue`. Empty
      (default) means no one can reach it — the dashboard is invisible
      to non-operators and to anonymous visitors. Lowercase, trim, and
      include all operator/founder addresses.

- [ ] Set `ADMIN_WEEKLY_DIGEST_ENABLED=true` on the deploy once
      `ADMIN_EMAILS` is wired and at least one live mail send has been
      verified end-to-end (existing transactional-email provider — see
      "OAuth & third-party APIs" below). Defaults are `0 0 9 ? * MON`
      UTC and override via `ADMIN_WEEKLY_DIGEST_CRON` /
      `ADMIN_WEEKLY_DIGEST_ZONE` if a different cadence is wanted. With
      the flag off the digest service is still wired but no scheduler
      fires, so direct invocation from tests/admin tooling still works.

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
      Enterprise $99/mo (plus matching annual prices at 2 months free —
      Personal $90/yr, Team $290/yr, Enterprise $990/yr). Hand over the
      six price IDs as env vars: `STRIPE_PERSONAL_PRICE_ID`,
      `STRIPE_TEAM_PRICE_ID`, `STRIPE_ENTERPRISE_PRICE_ID`,
      `STRIPE_PERSONAL_ANNUAL_PRICE_ID`, `STRIPE_TEAM_ANNUAL_PRICE_ID`,
      `STRIPE_ENTERPRISE_ANNUAL_PRICE_ID`. Annual IDs are optional — the
      checkout silently degrades to monthly when an annual SKU isn't set.
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

## Launch / marketing

- [ ] Record a 60–90 second Loom or YouTube demo of an email thread
      transforming into the chat view; supply the file or URL for the
      landing page hero embed.
- [ ] Draft Product Hunt launch assets (tagline, description, gallery)
      and schedule launch for a Tuesday 12:01 AM PT after MVP is live.
- [ ] Create the @MailIM Twitter/X account and a public Slack or Discord
      community; provide invite link for the footer.
