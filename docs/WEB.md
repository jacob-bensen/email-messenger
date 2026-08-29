# WEB.md — Thymeleaf public website inventory

The Spring app serves the entire ConexusMail website. Routes are grouped
by controller below. Fragments live under
`src/main/resources/templates/fragments/`; styles under
`src/main/resources/static/css/`.

All state-changing forms rely on the Spring Security session cookie
plus a CSRF token; all `POST` handlers listed below expect that token.
Business logic **always** runs through the service layer — controllers
must not duplicate rules the mobile REST API will need. When the mobile
API grows an endpoint that overlaps with a page below, both call the
same service.

## Marketing / auth / legal

- `GET  /` — `LandingController` → `landing.html`.
- `GET  /pricing` — `PricingController` → `pricing.html`.
- `GET  /demo` — `DemoController` → static demo of the chat UI.
- `GET  /login`, `POST` via Spring Security — `AuthController`.
- `GET  /register`, `POST /register` — `AuthController`.
- `GET  /verify-email` — `EmailVerificationController`.
- `GET  /password/forgot`, `POST /password/forgot`,
  `GET /password/reset`, `POST /password/reset` — `PasswordResetController`.
- `GET  /auth/google/start`, `GET /callback` — `OAuthStartController` +
  Spring Security OAuth2 client.
- `GET  /terms`, `GET /privacy`, `GET /refund` — `LegalController`.

## Account settings

- `GET  /account` — `AccountController` → `account.html` (billing summary + profile).
- `POST /account/email`, `POST /account/password` — `AccountController`.
- `GET  /account/appearance`, `POST /account/appearance` — theme + density.
- `GET  /account/notifications`, `POST /preferences` — digest opt-in.
- `GET  /account/privacy` — data + delete-account controls.

## Mailboxes

- `GET  /mailboxes` — `MailboxController` → `mailboxes/index.html`.
- `GET  /mailboxes/new`, `POST /mailboxes/new` — add IMAP-password account.
- `GET  /mailboxes/gmail/connect` — start Gmail OAuth.
- `GET  /mailboxes/gmail/reconnect/{id}` — refresh a stale OAuth token.

## Chats workspace (the product)

- `GET  /chats` — `ConversationController` → `chat.html` with the rail
  of conversationKey-keyed chats and either an empty pane or the
  most-recent conversation.
- `GET  /chats/{key}` — same page focused on one conversation.
- `GET  /chats/{key}/pane` — chat-pane fragment for in-place morphs.
- `GET  /chats/new`, `POST /chats/new` — start a new conversation.
- `POST /chats/{key}/send` — send a reply via SMTP.
- `POST /chats/refresh` — trigger a poll now.
- `GET  /chats/events` — SSE stream of `NewMailEvent`s
  (`ChatEventsController`).

## Billing

- `POST /billing/checkout` — Stripe Checkout session.
- `POST /billing/portal` — Stripe Customer Portal.
- `GET  /billing/success`, `GET /billing/cancel` — post-checkout landings.
- `GET  /billing/cancel-subscription`, `POST /billing/cancel-subscription`
  — self-serve cancel flow with reason capture.
- `POST /billing/webhook` — Stripe webhook (the one non-HTML endpoint).

## Teams

- `GET  /team/invite`, `POST /team/invite`,
  `GET /team/invite/accept`, `POST /team/invite/accept` —
  `TeamInviteController`.

## Admin

- `GET  /admin/revenue` — `AdminRevenueController`
  (gated by `AdminAuthorizer` + `AdminProperties.emails`).
- `POST /admin/revenue/reconcile-billing-period`,
  `POST /admin/retention/win-back` — admin actions.

## Digest / email

- `GET /digest/opt-out` — `DigestController`.

## PWA + SEO plumbing

- `GET /manifest.webmanifest`, `GET /sw.js`, `GET /offline`,
  `GET /apple-touch-icon.png`, `GET /icons/*.png`,
  `GET /images/og-card.png` — `PwaController`.
- `GET /robots.txt`, `GET /sitemap.xml` — `SeoController`.

## Legacy redirects

- `GET /threads`, `/threads/**`, `/senders` — `ChatRedirectController`
  → `/chats`.

## Shared fragments

`fragments/app-nav.html`, `fragments/settings-nav.html`,
`fragments/chat-pane.html`, `fragments/live-refresh.html`,
`fragments/sw-register.html`, `fragments/install-banner.html`,
`fragments/cookie-banner.html`, `fragments/seo.html`.
