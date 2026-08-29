# WEB — Thymeleaf site

Every user-visible page in the running product is server-rendered
Thymeleaf from `src/main/resources/templates/`. The mobile client is
separate (see `docs/API.md`); the web app and the future API share the
same service layer, never a fork of business logic.

## Page inventory (as of 2026-08-29)

Grouped by controller. Path → template. Everything under
`ConversationController`, `MailboxController`, `SavedSearchController`,
`AccountController`, `AdminRevenueController`, `TeamInviteController`,
`DigestController`, `BillingController`, `EmailVerificationController`
and `PasswordResetController` is authenticated by session cookie.

### Marketing / public

- `GET /` — landing (`PricingController`).
- `GET /pricing` — pricing (`PricingController`).
- `GET /demo` — interactive demo (`DemoController`).
- `GET /privacy`, `GET /terms`, `GET /refund` — legal boilerplate
  (`LegalController`, sources under `src/main/resources/legal/`).
- `GET /robots.txt`, `GET /sitemap.xml`, `GET /images/og-card.png`
  (`SeoController`).
- `GET /manifest.webmanifest`, `GET /sw.js`, `GET /offline`,
  `GET /icons/*` (`PwaController`).

### Auth

- `GET|POST /login`, `GET|POST /register` (`AuthController`).
- `GET /verify-email`, `POST /verify-email/resend`
  (`EmailVerificationController`).
- `GET /password/forgot`, `POST /password/forgot`, `GET /password/reset`,
  `POST /password/reset` (`PasswordResetController`).
- `GET /auth/google/start` (`OAuthStartController`) → Spring Security
  OAuth2 client handshake → `/login/oauth2/code/google`.

### Signed-in app — chats

- `GET /chats` — chats workspace (rail + open pane).
- `GET /chats/new`, `POST /chats/new` — compose new conversation.
- `POST /chats/refresh` — force poll.
- `GET /chats/{key}` — one conversation.
- `GET /chats/{key}/pane` — HTMX-style fragment for the open pane.
- `POST /chats/{key}/send` — send a reply.
- `GET /chats/events` — server-sent-event stream driving in-place
  refresh (`ChatEventStream` + `ChatEventsController`).
- Legacy: `GET /threads`, `/threads/**`, `/senders` all redirect to the
  matching `/chats` URL via `ChatRedirectController`.

### Signed-in app — mailboxes

- `GET /mailboxes` — list.
- `GET /mailboxes/new`, `POST /mailboxes` — connect a new IMAP or Gmail
  account.
- `POST /mailboxes/{id}/sync` — force a sync.
- `POST /mailboxes/preferences` — polling preferences.
- `POST /mailboxes/{id}/signature` — save signature.
- `POST /mailboxes/{id}/delete` — remove account.
- `GET /mailboxes/gmail/connect`, `GET /mailboxes/gmail/reconnect/{id}`,
  `GET /mailboxes/gmail/callback` — Gmail OAuth (`GmailConnectController`).

### Signed-in app — searches / account / digest / teams

- `POST /searches`, `POST /searches/{id}/delete` (`SavedSearchController`).
- `GET /account` and `/account/notifications`, `/account/appearance` (GET/POST),
  `/account/privacy`, `POST /account/password`, `POST /account/email`
  (`AccountController`).
- `GET /digest/opt-out` (`DigestController`).
- `GET|POST /team/invite`, `GET|POST /team/invite/accept`
  (`TeamInviteController`).

### Billing

- `POST /billing/checkout` — create Stripe Checkout Session.
- `POST /billing/portal` — Stripe Customer Portal redirect.
- `GET /billing/success`, `GET /billing/cancel` — post-checkout landings.
- `GET|POST /billing/cancel-subscription` — self-serve cancel flow.
- `POST /billing/webhook` — Stripe webhook (`@RestController`,
  signature-verified).

### Admin (operator-only, gated by `ADMIN_EMAILS`)

- `GET /admin/revenue` (`AdminRevenueController`).
- `POST /admin/revenue/reconcile-billing-period`.
- `POST /admin/retention/win-back` — EPIC-18 per-row CTA.

## Shared fragments / layout

- `templates/fragments/*.html` — nav, footer, flashes, PWA head, live
  refresh scripts (`fragments/live-refresh.html`), etc.
- `NavModelAdvice`, `SiteModelAdvice` — `@ControllerAdvice` that populate
  nav state and site branding on every page.

## Business logic must not fork

Web templates render; controllers coerce; services decide. When a REST
endpoint is added for the mobile client it MUST call the same service
that the matching Thymeleaf controller calls — never duplicate
validation or state mutation into a new class.
