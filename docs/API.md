# email-messenger — API Contract

**The canonical contract between the backend (`email-messenger`) and
the mobile client (`../conexus-mail-mobile`).**

Both sides depend on this file. If the backend changes shape here
without updating the client — or the client changes shape without
updating the backend — the contract is broken and must be fixed in
the same run.

Two surfaces are documented:

1. **HTTP surface today** — what the running backend actually exposes,
   including one JSON endpoint (`POST /chats/refresh`), one SSE stream
   (`GET /chats/events`), the Stripe webhook, and the Thymeleaf HTML
   routes (listed for completeness — the mobile client does not call
   them; it navigates to them in the system browser only for
   billing/mailbox setup).
2. **Planned mobile REST surface (`/api/mobile/v1/**`)** — the JSON
   API the mobile client will use. Every field is authoritative; when
   an endpoint moves from "planned" to "implemented" it stays in the
   same section and gets a ✅ marker.

Media types and conventions used throughout:

- Request/response bodies: `application/json; charset=utf-8` unless
  otherwise stated.
- Timestamps: ISO-8601 UTC, e.g. `"2026-08-29T14:03:11Z"`.
- IDs: opaque strings; do not assume format on the client.
- Errors: JSON `{"error": "<code>", "message": "<human-readable>"}`
  with the HTTP status carrying severity. Codes are stable; messages
  may change.
- Auth for `/api/mobile/v1/**`: `Authorization: Bearer <token>`
  header. A 401 always means "clear the token and re-authenticate".

---

## 1. HTTP surface today

### 1.1 SSE — `GET /chats/events`

- Auth: web session (cookie).
- Response: `text/event-stream`.
- Events emitted:
  - **`new-mail`** — `data: {"imported": <int>}` when new messages
    are imported for the signed-in user (fanned out via
    `NewMailEvent`). Keepalive comment lines (`: ping`) every ~25s.
  - **`(implicit close after 30 min)`** — clients reconnect
    transparently.
- Mobile does NOT call this — see the planned
  `GET /api/mobile/v1/events` for the token-authenticated equivalent.

### 1.2 JSON — `POST /chats/refresh`

- Auth: web session (Principal).
- Request body: none.
- Response: `200 OK` `{"imported": <int>}` — count of new messages
  imported by this synchronous poll.

### 1.3 `POST /billing/webhook`

- Auth: **not session-authed** — verified by `Stripe-Signature`
  header against `STRIPE_WEBHOOK_SECRET`.
- Request body: raw Stripe event JSON.
- Response: `text/plain` `"ok"` (200), `"invalid signature"` (400),
  `"error"` (500), or `"stripe disabled"` (503) if Stripe env vars
  are unset.
- Events handled: `checkout.session.completed`,
  `customer.subscription.{created,updated,deleted}`. Everything else
  returns 200 with no side effect.

### 1.4 HTML routes (website — for reference; mobile links out only)

Full inventory lives in `docs/WEB.md`. Highlights the mobile app
might deep-link to in the system browser:

- `/mailboxes/new` — connect a mailbox (IMAP form).
- `/mailboxes/gmail/connect` — Gmail OAuth flow.
- `/billing/checkout` and `/billing/portal` — Stripe.
- `/account`, `/account/notifications`, `/account/appearance`,
  `/account/privacy` — account settings.
- `/team/invite` — team management.
- `/searches` — saved searches.
- `/admin/revenue` — operator-only.

---

## 2. Planned mobile REST surface — `/api/mobile/v1/**`

**Status legend:** ⏳ planned · ✅ implemented · 🚧 in progress.

All endpoints below require `Authorization: Bearer <token>` except
`POST /auth/login`, `POST /auth/register`, and
`POST /push/webhook/...` (none of which are implemented yet). Every
endpoint MUST reuse the same service method that the corresponding
website controller calls; controllers here are thin adapters that
serialize the same domain result.

### 2.1 Auth

⏳ **`POST /api/mobile/v1/auth/login`**

Request:
```json
{"email": "ada@example.com", "password": "hunter2"}
```
Response `200`:
```json
{
  "token": "<opaque>",
  "expiresAt": "2026-09-05T14:03:11Z",
  "me": { ...see §2.2... }
}
```
Errors: `401 {"error":"invalid_credentials"}`,
`423 {"error":"email_not_verified"}`,
`429 {"error":"rate_limited"}`.

⏳ **`POST /api/mobile/v1/auth/logout`** — revokes the current token,
returns `204`.

⏳ **`POST /api/mobile/v1/auth/google/exchange`**

Client obtains a Google auth code via platform AppAuth/SIWG, POSTs
`{"code": "...", "codeVerifier": "..."}`, receives the same
`{token, expiresAt, me}` envelope as `/auth/login`. Server exchanges
the code, ensures a user row, and issues a mobile session token.

### 2.2 Me

⏳ **`GET /api/mobile/v1/me`**

Response `200`:
```json
{
  "id": "u_...",
  "email": "ada@example.com",
  "displayName": "Ada Lovelace",
  "plan": {
    "code": "PRO",
    "renewsAt": "2026-09-15T00:00:00Z",
    "features": {
      "mailboxes.max": 5,
      "idle.enabled": true,
      "attachments.download": false,
      "team.enabled": false
    }
  },
  "unreadCount": 12,
  "mailboxes": [
    {"id":"m_...","email":"ada@work.com","authType":"IMAP","lastSyncedAt":"..."}
  ]
}
```

`plan.features` is the resolved feature-flag block: the mobile client
NEVER re-derives what a plan allows. Adding a plan capability adds a
key here; the client obeys.

### 2.3 Chats list

⏳ **`GET /api/mobile/v1/chats?limit=50&cursor=<opaque>`**

Response `200`:
```json
{
  "items": [
    {
      "conversationKey": "ck_...",
      "participants": [
        {"email":"ada@work.com","displayName":"Ada","avatarInitials":"AL"},
        {"email":"chuck@babbage.co","displayName":"Charles Babbage","avatarInitials":"CB"}
      ],
      "lastMessage": {
        "id":"msg_...",
        "senderEmail":"chuck@babbage.co",
        "preview":"Meet Tuesday?",
        "sentAt":"2026-08-29T13:59:00Z",
        "outbound": false
      },
      "unread": true,
      "unreadCount": 2
    }
  ],
  "nextCursor": null
}
```

### 2.4 Chat view

⏳ **`GET /api/mobile/v1/chats/{conversationKey}?before=<opaque>&limit=100`**

Response `200`:
```json
{
  "conversationKey": "ck_...",
  "participants": [ ... same shape as §2.3 ... ],
  "messages": [
    {
      "id": "msg_...",
      "senderEmail": "chuck@babbage.co",
      "sentAt": "2026-08-29T13:59:00Z",
      "bodyText": "Meet Tuesday?",
      "bodyHtml": "<p>Meet Tuesday?</p>",
      "outbound": false,
      "deliveryState": "DELIVERED",
      "attachments": [
        {"id":"att_...","filename":"agenda.pdf","mime":"application/pdf","sizeBytes":13209}
      ],
      "threadHint": "#2"
    }
  ],
  "hasMoreBefore": false
}
```

`bodyText` is already quoted-reply-stripped by the server IM
transform; the client does no further stripping. Same-sender runs
are NOT pre-grouped here — grouping is presentational and lives in
`lib/shared/bubbles/`.

`deliveryState` values: `SENDING` (client-only optimistic),
`SENT`, `DELIVERED`, `FAILED`. `SENDING` and `FAILED` may be assigned
locally by the client for its own outbound optimistic bubble; every
other value comes from the server.

⏳ **`POST /api/mobile/v1/chats/{conversationKey}/reply`**

Request:
```json
{"bodyText": "Sounds good — Tuesday at 3?"}
```
Response `201`:
```json
{"message": { ...same shape as one item in §2.4 messages... }}
```
Errors: `403 {"error":"plan_limit"}` (send exceeded quota),
`404 {"error":"chat_not_found"}`,
`503 {"error":"smtp_unavailable"}`.

⏳ **`POST /api/mobile/v1/chats/{conversationKey}/read`** — marks the
conversation read for the signed-in user. Response `204`.

### 2.5 New chat

⏳ **`GET /api/mobile/v1/participants?q=<prefix>`** — returns up to 20
prior participants matching the prefix on email or display name.

⏳ **`POST /api/mobile/v1/chats/new`**

Request:
```json
{"toEmails": ["ada@work.com"], "subject": "Coffee?", "bodyText": "..."}
```
Response `201` — same envelope as `POST .../reply` above. If a
conversationKey for the participant set already exists, the message
is threaded into it; the response's `message.conversationKey` tells
the client where to navigate.

### 2.6 Live events

⏳ **`GET /api/mobile/v1/events`** — SSE, bearer-authed. Events:

- `new-mail` — `{"conversationKey":"ck_...","messageId":"msg_..."}`
- `mail-read` — `{"conversationKey":"ck_...","by":"self"|"other"}`
- `delivery-update` — `{"messageId":"msg_...","state":"DELIVERED"|"FAILED"}`

Keepalive comment `: ping` every ~25s. Timeout ~30 min; client
reconnects.

### 2.7 Push registration

⏳ **`POST /api/mobile/v1/push/register`**

Request:
```json
{
  "platform": "APNS" | "FCM",
  "token": "...",
  "appVersion": "1.0.0",
  "locale": "en-US"
}
```
Response `204`. Idempotent per (userId, platform, token).

⏳ **`POST /api/mobile/v1/push/unregister`** — inverse; response `204`.

---

## Non-goals for `/api/mobile/v1`

The following stay on the web only, and the mobile client either
deep-links into the browser or hides them entirely:

- Stripe checkout / portal / cancellation.
- Admin/operator dashboards.
- Team invite acceptance flow.
- Saved-search management UI (the saved-search *results* may become a
  read-only mobile surface later; management stays on web).
- Digest email opt-out (comes as a one-tap link in the email itself).

## Change process

Any change to a section under §2 requires:

1. Edit this file first, in the same commit.
2. Update `../conexus-mail-mobile/lib/data/api/` DTOs to match.
3. Update the backend controller + service to match.
4. If the shape change is breaking (removed field, changed type), the
   mobile client's minimum-supported version bumps and the server
   keeps the old field until the app-store rollout is complete.
