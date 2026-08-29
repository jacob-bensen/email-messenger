# Website (Thymeleaf) inventory

The Spring Boot app also serves the public marketing site AND the
authenticated web workspace. Templates live in
`src/main/resources/templates/`, static assets in
`src/main/resources/static/`. Controllers are `@Controller` classes that
return template names; they delegate all business logic to the same
services the mobile REST API uses (see `docs/API.md`).

## Fragments & layout

`templates/fragments/` holds shared header, footer, PWA meta, nav, and
`live-refresh.html` (the SSE glue that keeps `/chats` in sync). The
top-nav model is populated by `NavModelAdvice`; site-wide props by
`SiteModelAdvice`.

## Public / marketing pages

| Route      | Template          | Controller           |
| ---------- | ----------------- | -------------------- |
| `/`        | `landing.html`    | `PricingController`  |
| `/pricing` | `pricing.html`    | `PricingController`  |
| `/demo`    | (demo view)       | `DemoController`     |
| `/privacy` | `legal.html`      | `LegalController`    |
| `/terms`   | `legal.html`      | `LegalController`    |
| `/refund`  | `legal.html`      | `LegalController`    |

## Auth pages

| Route                 | Template               | Controller                     |
| --------------------- | ---------------------- | ------------------------------ |
| `/login`              | `login.html`           | `AuthController`               |
| `/register`           | `register.html`        | `AuthController`               |
| `/verify-email`       | `verify-email.html`    | `EmailVerificationController`  |
| `/password/forgot`    | `password/forgot.html` | `PasswordResetController`      |
| `/password/reset`     | `password/reset.html`  | `PasswordResetController`      |
| `/auth/google/start`  | (redirect)             | `OAuthStartController`         |

## Account settings

`AccountController` and friends:

- `/account` — profile + billing summary
- `/account/notifications` — email digest / re-engagement prefs
- `/account/appearance` — theme + accent colors
- `/account/privacy` — password + email change

## Mailbox connect

`MailboxController` (root `/mailboxes`), `GmailConnectController`
(root `/mailboxes/gmail`):

- `GET /mailboxes/new` — IMAP + provider chooser
- `POST /mailboxes/{id}/sync` — force a poll
- `POST /mailboxes/preferences` — sync cadence, per-account defaults
- `POST /mailboxes/{id}/signature` — per-account signature
- `POST /mailboxes/{id}/delete`
- `GET /mailboxes/gmail/connect` — begin OAuth
- `GET /mailboxes/gmail/reconnect/{id}` — refresh-token weekly reconnect
- `GET /mailboxes/gmail/callback` — OAuth code exchange

## Chats workspace

`ConversationController` (root `/chats`). This is the texting-style app:

- `GET /chats` — conversation list keyed by participant set
- `GET /chats/new` — compose form
- `POST /chats/new` — send a new message (creates a conversation)
- `POST /chats/refresh` (JSON) — trigger a poll, return unread counts
- `GET /chats/{key}` — full conversation view
- `GET /chats/{key}/pane` — pane-only fragment (for SSE swaps)
- `POST /chats/{key}/send` — reply into an existing conversation
- `GET /chats/events` — SSE stream of `NewMailEvent` (see
  `ChatEventStream`, `ChatEventsController`)

## Saved searches

`SavedSearchController` under `/saved-searches`:

- `POST /saved-searches/{id}/delete`
- (list + create are elsewhere; TODO in `INTERNAL_TODO.md` to confirm)

## Teams

`TeamInviteController`:

- `GET/POST /team/invite`
- `GET/POST /team/invite/accept`

## Billing

`BillingController`, `BillingWebhookController`:

- `POST /billing/checkout` — create Stripe session
- `POST /billing/portal` — open customer portal
- `GET /billing/success` / `GET /billing/cancel`
- `GET/POST /billing/cancel-subscription`
- `POST /webhooks/stripe` — Stripe event ingest (only `@RestController`
  in the app today besides `ConversationController#refresh`)

## Admin

`AdminRevenueController` under `/admin`:

- `GET /admin/revenue`
- `POST /admin/revenue/reconcile-billing-period`
- `POST /admin/retention/win-back`

Additional admin dashboards ride on the same `admin/` services (churn,
funnels, weekly digest). Access is gated by `AdminAuthorizer`.

## Digest

`DigestController`:

- `GET /digest/opt-out` — one-click token-based opt-out

## Rule for adding a new page

Reuse the same service class the REST API uses. Never re-implement
authorization, plan limits, or the IM transform inside a template or a
controller — those live in `service/`, `billing/`, and `auth/`.
