# API

Canonical REST API contract for the `conexus-mail-mobile` Flutter
client. Both repos depend on this file — every change here must land
in the same run as the matching server implementation and mobile
DTO/networking change.

## Status: no REST endpoints yet

As of 2026-08-29 the backend has ZERO endpoints intended for the
mobile client. Every route under `com.emailmessenger.web.*` renders
Thymeleaf HTML, and the only `@RestController` in the tree is the
Stripe webhook (`billing/BillingWebhookController`) which is not
mobile-facing.

The mobile client cannot make useful requests yet. Bringing this file
to life is tracked in `INTERNAL_TODO.md` and will be worked in
future sync runs.

## Design principles (when we start adding endpoints)

- All mobile endpoints live under `/api/v1/*`. The version segment is
  mandatory so a future breaking change can ship a `/api/v2/*`
  alongside the old one.
- Content type is `application/json` on both sides. Field names use
  `camelCase`. Timestamps are ISO-8601 UTC (`2026-08-29T14:03:11Z`).
- Every endpoint MUST call the same service the equivalent Thymeleaf
  page calls (`ConversationService`, `BillingService`, etc.). No
  business logic in controllers or DTOs.
- Authentication: bearer token in `Authorization: Bearer <token>`.
  Tokens are issued by `POST /api/v1/auth/login` (email + password)
  or `POST /api/v1/auth/oauth/google` (Google ID token). Refresh
  token flow at `POST /api/v1/auth/refresh`. Session cookies are
  NOT used for mobile — the mobile client never joins the Thymeleaf
  session.
- Errors follow a single envelope:
  ```json
  { "error": { "code": "STRING_CODE", "message": "human readable",
                "details": { /* optional */ } } }
  ```
- 401 = unauthenticated (client should refresh or re-login);
  403 = authenticated but not entitled (plan limit, admin only);
  404 = not found; 409 = conflict (idempotency-key replay,
  concurrent-edit); 422 = validation error;
  500 = server bug.
- Every mutation accepts an optional `Idempotency-Key` header. When
  present, the server records the (user, key) tuple and replays the
  original response on a retry. Mobile MUST send one for every
  send-message / reply / create-mailbox call.

## Endpoint plan (none of these ship yet — placeholders)

The following are targets, not implementations. Do not tell the mobile
client that any of these exist until the server-side controller +
tests are merged.

### Auth

- `POST /api/v1/auth/login` — email + password → access + refresh
  token.
- `POST /api/v1/auth/oauth/google` — Google ID token → access +
  refresh token (auto-provisions via `OAuth2ProvisioningService`).
- `POST /api/v1/auth/refresh` — refresh token → new access token.
- `POST /api/v1/auth/logout` — revokes the refresh token.
- `GET  /api/v1/me` — current user + subscription snapshot.

### Chats (conversation list + messages)

- `GET  /api/v1/chats?cursor=…` — paginated list keyed by
  `conversationKey` (mirrors `/chats`).
- `GET  /api/v1/chats/{key}` — one conversation with its bubble
  runs (mirrors `/chats/{key}`).
- `GET  /api/v1/chats/{key}/messages?before=…` — older messages.
- `POST /api/v1/chats/{key}/send` — send a reply through
  `OutboundMessageService`.
- `POST /api/v1/chats/{key}/read` — mark read.

### Mailboxes

- `GET  /api/v1/mailboxes` — connected accounts.
- `POST /api/v1/mailboxes/imap` — connect IMAP-password.
- `POST /api/v1/mailboxes/gmail/oauth-code` — exchange a Google
  auth-code from the mobile OAuth flow.
- `DELETE /api/v1/mailboxes/{id}` — disconnect.
- `POST /api/v1/mailboxes/{id}/sync` — force-poll.

### Live delivery

- `GET /api/v1/chats/stream` — Server-Sent Events feed of
  `NewMailEvent`s scoped to the authenticated user, mirroring what
  the web app receives on `/chats/events`.

### Billing

- `GET  /api/v1/billing/plan` — current plan + limits.
- `POST /api/v1/billing/checkout` — returns a Stripe Checkout URL
  the mobile client opens in an in-app browser.
- `POST /api/v1/billing/portal` — returns a Stripe Billing Portal
  URL.

## DTO conventions

- Server DTOs are Java records in `com.emailmessenger.web.api.dto`
  (package-private unless the SDK needs them).
- Mobile DTOs are Dart classes in
  `lib/data/api/dto/` on the client side. Field name and shape MUST
  match byte-for-byte.
- Nullable fields on either side are explicit — never rely on the
  absence of a key to mean null; send `null` explicitly. Dart null-
  safety is on; Java DTOs use `@JsonInclude(Include.ALWAYS)` on any
  nullable-but-required field.
