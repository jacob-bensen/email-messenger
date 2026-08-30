# API.md — ConexusMail API contract

**Status: the mobile-facing JSON REST API is TBD.** Today the backend is a
Thymeleaf webapp; the only endpoints that exist are HTML pages, HTML
form-post handlers, an SSE stream, and the Stripe webhook.

This file is the **single source of truth** the mobile client and the
website must both conform to once the JSON API lands. Any endpoint added
here must be implemented by the backend AND consumed by the mobile client
in the same run.

## Conventions (once the JSON API exists)

- Base path: **`/api/v1`** (TBD — not yet mounted).
- Content type: `application/json; charset=utf-8` for request and response.
- Auth: **TBD.** Session cookies work for the website but are unsuitable
  for a mobile app. A bearer-token scheme (opaque device tokens issued at
  login, revocable per device, stored in the client's secure storage) is
  the presumed direction; not yet built.
- Errors: `application/problem+json` (RFC 7807) with `type`, `title`,
  `status`, `detail`, and optional `errors` array for field-level
  validation. Not yet standardized on the server — `GlobalExceptionHandler`
  today returns HTML error pages.
- Timestamps: ISO-8601 UTC (`2026-08-30T14:23:45Z`).
- Pagination: cursor-based (`?cursor=…&limit=…`); response includes
  `nextCursor` when more remain. Not yet implemented.

## Endpoint surface (planned, TBD unless marked)

### Auth
- `POST /api/v1/auth/login` — email + password → `{ token, user }`. TBD.
- `POST /api/v1/auth/logout` — revoke current device token. TBD.
- `POST /api/v1/auth/google` — exchange Google ID token → device token. TBD.
- `GET  /api/v1/me` — current user + plan summary. TBD.

### Chats (participant-set conversations)
- `GET  /api/v1/chats` — list conversations for the signed-in user. TBD.
- `GET  /api/v1/chats/{conversationKey}` — full conversation
  (messages, participants, signature card). TBD.
- `POST /api/v1/chats/{conversationKey}/messages` — send a reply. TBD.
- `POST /api/v1/chats` — start a new chat with recipients + first message. TBD.
- `POST /api/v1/chats/{conversationKey}/read` — mark read. TBD.

### Mailboxes
- `GET  /api/v1/mailboxes` — user's connected accounts. TBD.
- `POST /api/v1/mailboxes` — add IMAP-with-password account. TBD.
- `POST /api/v1/mailboxes/{id}/sync` — trigger an ad-hoc fetch. TBD.
- `POST /api/v1/mailboxes/{id}/signature` — update outbound signature. TBD.
- `DELETE /api/v1/mailboxes/{id}` — disconnect. TBD.
- (Gmail OAuth stays browser-side; mobile deep-links out to the web flow.)

### Live delivery
- `GET  /api/v1/events` — SSE (or WebSocket) stream of `NewMailEvent` etc.
  TBD; the browser today uses `/chats/events`.

### Push notifications (mobile-only)
- `POST /api/v1/push/subscriptions` — register FCM/APNs token. TBD.
- `DELETE /api/v1/push/subscriptions/{id}`. TBD.

### Saved searches
- `GET / POST / DELETE /api/v1/searches`. TBD.

---

## What actually exists today (website / non-mobile)

These are the existing routes; mobile should **not** call them directly.
They are here so the sync audit can catch drift when the JSON API is added.

### Session-cookie form/HTML routes

| Method | Path | Handler |
| --- | --- | --- |
| GET  | `/`, `/pricing` | `PricingController` |
| GET  | `/demo` | `DemoController` |
| GET/POST | `/login`, `/register` | `AuthController` |
| GET  | `/auth/google/start` | `OAuthStartController` |
| GET/POST | `/verify-email`, `/verify-email/resend` | `EmailVerificationController` |
| GET/POST | `/password/forgot`, `/password/reset` | `PasswordResetController` |
| GET  | `/account`, `/account/notifications`, `/account/appearance`, `/account/privacy` | `AccountController` |
| POST | `/account/appearance`, `/account/password`, `/account/email` | `AccountController` |
| GET  | `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane` | `ConversationController` |
| POST | `/chats/new`, `/chats/refresh`, `/chats/{key}/send` | `ConversationController` |
| GET  | `/chats/events` (`text/event-stream`) | `ChatEventsController` |
| GET  | `/threads`, `/threads/**`, `/senders` (302 → `/chats`) | `ChatRedirectController` |
| GET  | `/mailboxes`, `/mailboxes/new` | `MailboxController` |
| POST | `/mailboxes`, `/mailboxes/{id}/sync`, `/mailboxes/preferences`, `/mailboxes/{id}/signature`, `/mailboxes/{id}/delete` | `MailboxController` |
| GET  | `/mailboxes/gmail/connect`, `/reconnect/{id}`, `/callback` | `GmailConnectController` |
| GET/POST | `/team/invite`, `/team/invite/accept` | `TeamInviteController` |
| POST | `/searches`, `/searches/{id}/delete` | `SavedSearchController` |
| GET  | `/privacy`, `/terms`, `/refund` | `LegalController` |
| GET  | `/digest/opt-out` | `DigestController` |
| POST | `/billing/checkout`, `/billing/portal`, `/billing/cancel-subscription` | `BillingController` |
| GET  | `/billing/success`, `/billing/cancel`, `/billing/cancel-subscription` | `BillingController` |
| GET  | `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | `PwaController` |
| GET  | `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | `SeoController` |
| GET  | `/admin/revenue`, POST `/admin/revenue/reconcile-billing-period`, POST `/admin/retention/win-back` | `AdminRevenueController` |

### JSON endpoints that exist

| Method | Path | Handler | Notes |
| --- | --- | --- | --- |
| POST | `/billing/webhook` | `BillingWebhookController` (`@RestController`) | Stripe → server; verifies signature via `STRIPE_WEBHOOK_SECRET`. Not a mobile-client endpoint. |

That is the whole JSON surface today.
