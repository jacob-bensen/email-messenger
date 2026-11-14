# ConexusMail API contract

The canonical contract between the backend (`email-messenger`) and every
machine client — the mobile app (`conexus-mail-mobile`), the website's own
JavaScript, and Stripe. Both repos depend on this file. Change it **before**
changing either implementation, and change both implementations in the same
run.

Status legend: **Implemented** = exists in code and is tested.
**Planned** = agreed direction, no code yet; shapes are not final until the
section is promoted to Implemented.

## 1. Current state (2026-11-14)

There is **no REST API for the mobile client yet**. The backend is a
server-rendered Thymeleaf site. The only machine-consumable endpoints are the
ones in §3, and they authenticate with the browser session cookie, which a
native client cannot use sensibly (see §2). The mobile app therefore makes no
network calls today. Building `/api/v1` (§5) is the first roadmap item in
`conexus-mail-mobile/docs/DESIGN.md`.

## 2. Authentication (Implemented: browser session only)

- Server-side `HttpSession`, cookie `JSESSIONID`. Form login `POST /login`
  (`email`, `password`, optional `remember-me`) or Google OIDC.
- An unauthenticated request to a protected path gets **302 → `/login`**,
  never 401 — including `fetch`/SSE callers.
- Every non-GET request needs a CSRF token (form field `_csrf`, or header
  `X-CSRF-TOKEN`), except `POST /billing/webhook`.
- There is **no** bearer token, API key, JWT, basic auth, or CORS
  configuration. Login is throttled (5 failures / 15 min per email or IP →
  redirect `/login?error=locked`).

## 3. Implemented machine endpoints

### POST /chats/refresh
Synchronously polls the signed-in user's mailboxes that have not synced in
the last minute. Used by the website's 60-second heartbeat.

- Auth: session cookie + CSRF header.
- Request body: none.
- `200 application/json`:
  ```json
  { "imported": 3 }
  ```
  `imported` (integer ≥ 0): messages imported across the user's mailboxes
  (received + sent). Per-mailbox IMAP failures are logged and count as 0.

### GET /chats/events
Server-Sent Events stream of new-mail notifications for the signed-in user.

- Auth: session cookie.
- `200 text/event-stream`. The server closes the stream after 30 minutes;
  clients reconnect.
- Event `new-mail`, data is JSON:
  ```
  event: new-mail
  data: {"imported":2}
  ```
  Sent after a poll commits with at least one imported message. It carries a
  count only — no conversation keys or message ids — so the client refetches.
- Comment line `:ping` every 25 s (`mailbox.sse.keepalive-ms`).
- No event ids, no replay of missed events. Fan-out is in-memory, so it
  works on a single backend instance only.

### GET /chats/{key}/pane
HTML fragment (not JSON) of one conversation, used by the website to swap
the open pane in place. Marks the conversation read. Returns `200` with a
"missing" fragment when the key is unknown. Not for mobile use.

### POST /billing/webhook
Stripe webhook receiver.

- Auth: `Stripe-Signature` header verified against
  `billing.stripe.webhook-secret`. Public, CSRF-exempt.
- Request: raw Stripe event JSON.
- Responses (`text/plain`): `200 ok`; `400 missing Stripe-Signature header`;
  `400 invalid signature`; `503 webhook not configured`; `500 error`.
- Handled event types: `checkout.session.completed`,
  `customer.subscription.created`, `customer.subscription.updated`,
  `customer.subscription.deleted`. Anything else is acknowledged with `200`.

### GET /manifest.webmanifest, GET /sw.js
PWA manifest (`application/manifest+json`: `name`, `short_name`,
`description`, `start_url` `/chats`, `scope` `/`, `display` `standalone`,
`orientation`, `lang`, `dir`, `theme_color` `#2f855a`, `background_color`
`#0f1512`, `icons[]`) and service worker. Public.

### GET /actuator/health
Spring Boot health, public: `{"status":"UP"}`.

### Errors
Every other failure on these endpoints is rendered as an **HTML** error page
(`error` template) with the status from `GlobalExceptionHandler`: 400
(`IllegalArgumentException`), 404 (unknown/not-owned resource), 409 (data
integrity), 413 (upload too large: 10 MB/file, 25 MB/request), 502
(mail or billing provider failure), 500. There is no JSON error envelope yet.

## 4. Shared rules (Implemented on both sides)

Rules the server owns and the client mirrors in `lib/domain/`. The server's
answer always wins.

### Conversation key
A conversation is all of one owner's threads that share a key; the key
identifies the set of non-owner people. Server: `ConversationKeyService`.
Client: `lib/domain/conversation_key.dart`.

1. Take every message's sender plus To/Cc recipients (Bcc excluded).
2. Trim (Java `String.trim()` semantics) and lowercase each address; drop
   blanks and the owner's own addresses (account email + every connected
   mailbox address, normalized the same way).
3. De-duplicate, sort ascending by UTF-16 code unit, join with `\n`. If
   nothing is left, use the sentinel `U+0000` followed by `self`.
4. SHA-256 the UTF-8 bytes; the key is the first **32 lowercase hex chars**.

Golden vectors (owner addresses = `me@example.com`), asserted by
`ConversationKeyServiceTest#goldenVectorsMatchTheMobileClientMirror` and
`test/domain/conversation_key_test.dart`:

| Emails | Key |
|---|---|
| `ada@acme.com` | `154e1e0a1872533ddf397a16e5a65dcf` |
| ` Bob@Acme.com`, `ada@acme.com`, `ME@example.com`, `ADA@ACME.COM ` | `487b97fee6493a379888b62e3969cb14` |
| `me@example.com`, blank | `95270985d1d4b88724c13887a7794ec3` |

### Conversation title and initials
Server: `ConversationLabels`. Client: `lib/domain/conversation_labels.dart`.
Title: `You` (no members), the single label, or the first three labels joined
with `, ` plus ` +N` for the rest. Initials: first letters of the first two
whitespace-separated words of the display name, uppercased; with no display
name, the first letter of the email local-part; otherwise `?`.

### Plan limits
Server: `PlanLimits` / `PlanLimitService`. Client:
`lib/domain/plan_limits.dart`. A plan counts only while the subscription
status is `active` or `trialing`; otherwise the user is FREE.

| Plan | Mailboxes | Threads | Saved searches |
|---|---|---|---|
| FREE | 3 | 500 | unlimited |
| PRO | 5 | unlimited | unlimited |
| BUSINESS | unlimited | unlimited | unlimited |

## 5. Planned: `/api/v1` for the mobile client (not implemented)

Nothing below exists. It records the intended direction so the first
feature run can write the real contract here before coding.

Open decisions (see `INTERNAL_TODO.md`):
- **Auth mechanism.** Proposed: opaque bearer tokens issued by
  `POST /api/v1/auth/login`, stored hashed (same pattern as the existing
  reset/verification tokens), in a separate stateless `SecurityFilterChain`
  for `/api/**` that returns 401 JSON instead of redirecting and has CSRF
  off. Google sign-in on mobile needs its own design.
- **Error envelope.** Proposed: `{"error": {"code": "...", "message": "..."}}`
  with stable codes, including `plan_limit_exceeded`.
- **Timestamps.** The server stores zone-less UTC `LocalDateTime`; the API
  should emit ISO-8601 UTC instants.

Intended resources, in build order:
1. Auth: login, logout, current user (`me`).
2. Conversations: paged list (query, mailbox, filter
   `ALL|UNREAD|AWAITING|ATTACHMENTS`, counts), one conversation with its
   bubble runs, mark read.
3. Send: reply into a conversation, start a new one.
4. Mailboxes: list, connect (IMAP password), sync, remove.
5. Live updates: SSE (or push) with enough detail to update without a
   full refetch.
