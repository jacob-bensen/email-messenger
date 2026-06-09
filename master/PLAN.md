# PLAN

## What this is

**email-messenger** (product name: MailIM) is a Spring Boot web app that
imports email threads over IMAP and renders them as a modern IM-style chat
view — bubbles, avatars, day separators, dark mode — instead of the
nested-quote wall most mail clients show. It is positioned as a freemium
SaaS: Free (1 mailbox, 500 threads, 1 saved search), Personal $9/mo
(3 mailboxes, unlimited history, unlimited saved searches),
Team $29/mo (10 mailboxes, sharing), Enterprise $99/mo (SSO, audit).
Annual billing offers 2 months free. Money comes from recurring
subscriptions, with natural Free → Personal → Team upgrades as mailbox
count, history, and saved-search counts grow. EPICs 02–12 (Monetization,
Mailbox Onboarding, Deployability, Acquisition, Launch readiness, Inbox
Search, Saved Searches & Reactivation, Account self-serve, Mobile/PWA,
Annual billing surfacing, First-paying-customer attribution funnel) are
code-complete in `claude_routine`; live deploy is gated on Master ops
(hosting, domain, Stripe live keys, encryption secrets, Google OAuth
credentials, demo video URL).

## Primary Objective

**Ship EPIC-13 Google OAuth signup.** Registration today forces every
visitor to invent and remember another password, then round-trip through
an email-verification link before they can connect a mailbox. That's the
single most-leveraged drop-off point in the funnel — every paying
customer has to clear it, and a meaningful share of intent-strong
visitors won't. "Continue with Google" collapses the password +
verification round-trips into one tap, and on mobile (where the install
prompt and PWA work has already opened that surface) Google's account
picker is muscle memory. The funnel dashboard from EPIC-12 already
attributes per-channel conversion rates from `users.acquisition_source`,
so once Google sign-up lands the operator can immediately see whether
removing the password friction actually moved the trial-rate and
paid-rate needles. This Objective ends when a visitor can click
"Continue with Google" from `/login`, `/register`, or
`/pricing?plan=personal&billing=annual`, complete Google's consent
screen, and land at Stripe Checkout (or `/threads`, if no plan is in
play) — without ever seeing a MailIM password prompt and without
clicking through a separate email-verification link.

## Milestones

1. **"Continue with Google" sign-in + auto-provision.** [shipped
   2026-06-09] Spring Security OAuth2 client wires the Google OIDC
   provider behind a `ClientRegistrationRepository` bean that's only
   created when `AUTH_GOOGLE_CLIENT_ID` + `AUTH_GOOGLE_CLIENT_SECRET`
   are both non-blank (so an unconfigured deploy still boots).
   `GoogleOidcUserService` calls `OAuth2ProvisioningService.provisionFromGoogle`
   on the OIDC callback to auto-create the `users` row with
   `acquisition_source="google"` and `email_verified_at` stamped from
   Google's `email_verified` claim (no separate verification round-trip).
   `/login` and `/register` render the brand-styled button only when
   the registration is live; an unconfigured deploy hides it.
2. **Carry plan + billing + utm_source through OAuth state.** [shipped
   2026-06-09] New `GET /auth/google/start` reads `plan`, `billing`,
   and `utm_source` off the click URL, validates them
   (`Plan.parse`/`BillingPeriod.parse`, utm trimmed and clamped to 64
   chars), stores them in the HTTP session under namespaced keys,
   then 302s to `/oauth2/authorization/google` — the buttons on
   `/login` and `/register` now point at this endpoint and carry the
   inbound params via Thymeleaf. `PlanCheckoutSuccessHandler` falls
   back to the session intent when the callback request has no plan
   param and clears it on consume so a stale intent can't follow the
   visitor into a later session. `GoogleOidcUserService` peeks the
   stored `utm_source` and threads it into
   `OAuth2ProvisioningService.provisionFromGoogle`, which credits the
   inbound channel on first provision (falling back to `"google"`)
   without touching the source on any existing row.
3. **Account linking for existing email-password users.** [shipped
   2026-06-09] Flyway V18 adds a nullable, uniquely indexed
   `users.google_subject` column. `OAuth2ProvisioningService.provisionFromGoogle`
   now accepts the OIDC `sub`; it looks subject up first, then falls
   back to email match — both miss creates a fresh row with the subject
   stamped, an email-match writes the subject onto the existing row
   (linking the password account), and a subject-match resolves the row
   even when the Google address has since changed. `GoogleOidcUserService`
   threads `delegate.getSubject()` through on every OIDC callback.
4. **Hide /password/forgot for Google-only users + helpful redirects.**
   [shipped 2026-06-09] Flyway V19 adds `users.password_set` (default
   TRUE for legacy email-password rows). `OAuth2ProvisioningService`
   stamps FALSE on a fresh Google-provisioned row, and
   `PasswordResetService.consumeToken` flips it back to TRUE so a user
   who later set a chosen password is no longer Google-only.
   `requestReset` now returns a tri-state `Outcome`
   (`SENT`/`IGNORED`/`GOOGLE_ONLY`) and `PasswordResetController`
   renders the new `status=google` branch on `forgot.html` — replacing
   the form with a "Continue with Google" button instead of mailing a
   reset link that would have minted a credential bypassing Google.
   New `LoginFailureHandler` swaps Spring Security's default
   `failureUrl("/login?error")` for a lookup-based redirect: failures
   for emails whose row has `google_subject` set go to
   `/login?error=google`, where `login.html` shows a "Did you mean to
   sign in with Google?" `alert-info` nudge above the form.

## Done means

A visitor lands on `/pricing?plan=personal&billing=annual`, clicks
"Continue with Google", consents on Google's screen, and arrives at
Stripe Checkout for the annual Personal plan — without typing a MailIM
password and without clicking an email-verification link. A new
paying customer from Google sign-up appears in EPIC-12's
`/admin/revenue` funnel attributed to their inbound `utm_source`
(falling back to `"google"` for direct Google sign-ups). An existing
email-password user can also click "Continue with Google" with the
same email and have the accounts transparently linked. A Google-only
user who clicks `Forgot password?` sees a "Sign in with Google" hint
instead of a dead-end reset email.
