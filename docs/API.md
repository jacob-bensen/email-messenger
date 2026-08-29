# API.md — canonical contract

> **Status: mostly TBD.** The backend today is fully server-rendered
> Thymeleaf. There is no dedicated REST surface for the mobile client
> yet. This document is the single source of truth for both repos: the
> Spring server owns it, the Flutter client (`conexus-mail-mobile`)
> consumes it. Every change here MUST be reflected in both
> implementations in the same run.

## Existing non-HTML endpoints

These already exist and the client-side implementations (browser JS or
Stripe) rely on them. Document them so we don't accidentally break
them.

### `POST /billing/webhook`

Stripe webhook receiver.

- Auth: public; verified by `Stripe-Signature` header
  (`StripeWebhookGateway.parse`).
- Request: raw JSON string (Stripe event payload). Header
  `Stripe-Signature` required.
- Responses:
  - `200 "ok"`
  - `400 "invalid signature"` — bad signature
  - `400 "missing Stripe-Signature header"`
  - `503 "webhook not configured"` — no signing secret configured
  - `500 "error"` — internal
- Controller: `com.emailmessenger.billing.BillingWebhookController#handle`

Not called by the mobile client. Do not repurpose.

### `POST /chats/refresh`

Foreground mailbox-refresh heartbeat used by the chat page's live-refresh
fragment. Triggers a synchronous poll of the current user's mailboxes.

- Auth: authenticated session (Spring Security).
- Request body: none.
- Response `200 application/json`:
  ```json
  { "imported": 3 }
  ```
  `imported` = number of newly imported messages during this call.
- Controller: `com.emailmessenger.web.ConversationController#refresh`

Not currently CSRF-exempt or token-authenticated; **not yet safe to
call from the mobile client** as-is (see "Mobile REST — TBD" below).

### `GET /chats/events` (SSE)

Server-Sent Events stream. Emits a frame each time
`MailboxPollingService` or `ImapIdleService` publishes a `NewMailEvent`
for the authenticated user.

- Auth: authenticated session.
- Produces: `text/event-stream`.
- Frame payload: event JSON (see `ChatEventStream` for the exact shape;
  today the browser fragment treats any frame as "re-fetch the chat
  list").
- Controller: `com.emailmessenger.web.ChatEventsController#events`

Mobile equivalent (SSE over a token-authenticated URL, or a WebSocket
push channel) is a Phase-1 target for the mobile client.

### Static PWA / SEO endpoints

`GET /robots.txt`, `/sitemap.xml`, `/manifest.webmanifest`, `/sw.js`,
`/icons/**`, `/apple-touch-icon.png`, `/offline`, `/images/og-card.png`
— served by `SeoController` and `PwaController`. Not part of the mobile
contract.

## Mobile REST — TBD (design tracker)

The mobile client (`conexus-mail-mobile`) does not exist yet. When it
lands, it will need a real JSON API. Design constraints:

- **Auth model must not rely on the Thymeleaf session cookie.** Options
  under consideration:
  1. Bearer-token endpoint (`POST /api/auth/token` with email +
     password, returns opaque token stored on device).
  2. OAuth device-code flow for Google-linked accounts.
  Chosen approach + refresh/rotation policy will be decided in the
  first mobile-auth iteration and recorded here.
- **All mobile endpoints will live under `/api/v1/*`** to keep them
  clearly separated from the Thymeleaf routes and to allow future
  versioning.
- **JSON responses use `snake_case` field names** (matches the mobile
  Dart convention `json_serializable` defaults to) — or `camelCase`,
  to be locked in when the first endpoint ships. Whichever is chosen,
  it is chosen for the whole surface and recorded here.
- **CSRF** is disabled for `/api/v1/**` and replaced by bearer-token
  auth.
- **Error shape** (proposed):
  ```json
  { "error": { "code": "plan_limit_exceeded", "message": "..." } }
  ```
  HTTP status carries the general class (4xx/5xx); `code` disambiguates.
- **Pagination** for list endpoints uses `limit` + a cursor (`after`)
  token; no offset pagination.

### Planned endpoints (not yet implemented — do not call)

Every row here maps 1-to-1 to a `lib/domain/` DTO in the mobile repo
when it's implemented. Ordered by rollout priority.

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/v1/auth/token` | Exchange email+password for a bearer token |
| `POST` | `/api/v1/auth/token/refresh` | Rotate a bearer token |
| `POST` | `/api/v1/auth/token/revoke` | Sign out this device |
| `GET`  | `/api/v1/me` | Current user + plan + entitlements |
| `GET`  | `/api/v1/mailboxes` | List connected mailboxes |
| `POST` | `/api/v1/mailboxes` | Add IMAP mailbox |
| `POST` | `/api/v1/mailboxes/{id}/sync` | Force a foreground sync |
| `DELETE` | `/api/v1/mailboxes/{id}` | Disconnect |
| `GET`  | `/api/v1/chats` | Paginated chat list keyed by `conversationKey` |
| `GET`  | `/api/v1/chats/{key}` | One chat pane (messages, participants) |
| `POST` | `/api/v1/chats/{key}/messages` | Reply into an existing chat |
| `POST` | `/api/v1/chats` | Compose a new chat |
| `GET`  | `/api/v1/events` (SSE) | Live event stream |
| `POST` | `/api/v1/push/subscriptions` | Register FCM/APNs token |

Every one is TBD; do not treat this table as a contract yet. Add the
request/response schema in the same commit that ships the endpoint.

## Change process

1. Edit this file **first** — request/response schema, auth, errors.
2. Implement the Spring controller + service.
3. Implement the Dart DTOs + networking call.
4. If web pages need the same behavior, update the Thymeleaf page and
   make it call the same service — never the API.
5. Both sides land in the same run.
