# API

**Canonical API contract** for the email-messenger backend, shared
between the backend and the `conexus-mail-mobile` Flutter client. Both
sides depend on this file — client and server implementations must
match it exactly. If code and this document disagree, one of them is a
bug; the code owns behavior, this document owns intent, and the fix is
to update whichever is wrong in the same PR.

---

## Status: no JSON REST API exists today

Every controller in `com.emailmessenger.web`, `.auth`, `.billing`,
`.team`, `.digest`, and `.admin` returns **Thymeleaf HTML** (or an SSE
stream, or a redirect). There is currently **no JSON endpoint** the
mobile client can call, no bearer-token auth, and no `/api` prefix.
The only non-HTML endpoints are:

| Method | Path | Media type | Purpose |
| :-- | :-- | :-- | :-- |
| `GET`  | `/chats/events`         | `text/event-stream` | SSE stream of `new-mail` events for the logged-in user. Session-cookie authenticated. |
| `GET`  | `/manifest.webmanifest` | `application/manifest+json` | PWA manifest. |
| `GET`  | `/sw.js`                | `application/javascript` | PWA service worker. |
| `GET`  | `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | text/xml/png | SEO/OG. |
| `POST` | `/billing/webhook`      | Stripe signed payload | Stripe webhook. Not for client use. |

Adding the mobile client is therefore a two-repo effort: the JSON API
described below has to be **built** in the backend before the mobile
side can consume it. This document is intentionally the design of what
we are building, not a description of what already exists.

Every unbuilt endpoint below carries an explicit **Status: TBD** note.
As each endpoint ships, that line becomes **Status: implemented at
`<controller#method>`** and any implementation choice that departs
from what's written here is reconciled in the same commit.

---

## Design principles

1. **Contract first.** Design changes land in this file before code
   changes to either repo. Same PR when possible; same run when the two
   PRs are the same run.
2. **Same service layer as the web app.** JSON controllers delegate
   through the exact `ConversationService`, `ReplyService`,
   `MailboxService`, `SubscriptionService` etc. methods that the
   Thymeleaf controllers use. **No forked business logic.**
3. **Server-authoritative.** The client never dictates state derived
   from other state (bubble groupings, quoted-reply stripping,
   read/unread counts, plan-limit enforcement). The client is free to
   *predict* locally for optimism; on reconcile, server wins.
4. **JSON DTOs are records** in `com.emailmessenger.api.dto`, mirrored
   1:1 by Dart records in `lib/domain/dto/`. Field names are
   `camelCase` in both. Timestamps are ISO-8601 UTC strings
   (`"2026-08-28T15:04:05Z"`). Enums serialize as the
   `SCREAMING_SNAKE_CASE` name of the Java enum constant.
5. **Errors.** All error responses use the shape
   `{ "error": { "code": "<slug>", "message": "<human>", "field": "<optional>" } }`
   with an appropriate HTTP status. `code` values are stable strings
   the client can branch on; `message` is display-safe but the client
   may localize by `code`.
6. **Pagination.** List endpoints take `?limit=<n>&cursor=<opaque>`
   and return `{ "items": [...], "nextCursor": "<opaque or null>" }`.
   Servers cap `limit`; a request over the cap is silently clamped.
7. **Idempotency.** Any state-mutating endpoint the client may retry
   (send message, connect mailbox) accepts an `Idempotency-Key` header
   and returns the same response for the same key within 24h.

---

## Base URL & versioning

- Development: `http://localhost:8080`
- Production: `https://<prod-domain>` (see `MASTER_ACTIONS.md` —
  domain choice is a human decision).
- All JSON endpoints live under **`/api/v1`**. Breaking changes
  require a new major prefix (`/api/v2`) alongside; minor changes are
  additive-only within the same version.

---

## Authentication (TBD)

The web app uses **session cookies**. The mobile client cannot rely on
that: it needs a token the app can store and attach to every request.

**Chosen approach (to be implemented):** short-lived opaque **access
tokens** (30 min) plus long-lived rotating **refresh tokens** (60
days), stored on device in the platform secure store (Keychain /
Keystore). Access tokens are attached as
`Authorization: Bearer <token>`. Refresh tokens are used only against
`POST /api/v1/auth/refresh` and are rotated on every use; the old
refresh token is invalidated. Server keeps a `refresh_tokens` table
keyed by `(user_id, jti)` so tokens can be revoked on logout or
password change.

Endpoints (all **Status: TBD**):

- `POST /api/v1/auth/login`
  - Body: `{ "email": string, "password": string }`
  - `200`: `{ "accessToken": string, "refreshToken": string,
    "expiresIn": 1800, "user": <User> }`
  - `401 invalid_credentials` on failure; `403 email_unverified` when
    the user hasn't verified their email yet.
- `POST /api/v1/auth/refresh`
  - Body: `{ "refreshToken": string }`
  - `200`: same shape as login (new access + rotated refresh).
  - `401 invalid_refresh_token`.
- `POST /api/v1/auth/logout`
  - Header: `Authorization: Bearer <access>`; Body:
    `{ "refreshToken": string }` — server invalidates that refresh
    token. `204`.
- `POST /api/v1/auth/google`
  - Body: `{ "idToken": string }` — verified against Google, mapped to
    an existing `User` by `google_subject` or by email match. Same
    response shape as login.
- `GET /api/v1/me`
  - `200`: `<User>` for the caller.

`<User>` DTO:

```json
{
  "id": 12345,
  "email": "person@example.com",
  "displayName": "Person Example",
  "plan": "PRO",              // enum: FREE | PRO | BUSINESS | ENTERPRISE
  "emailVerified": true,
  "createdAt": "2026-01-05T12:00:00Z"
}
```

---

## Mailbox connect (TBD)

- `GET  /api/v1/mailboxes` — list the caller's connected mailboxes.
- `POST /api/v1/mailboxes` — connect an IMAP-password mailbox
  (`{ host, port, username, password, useSsl }`).
- `POST /api/v1/mailboxes/{id}/sync` — force a pull now.
- `POST /api/v1/mailboxes/{id}/signature` — set the mailbox signature.
- `DELETE /api/v1/mailboxes/{id}` — disconnect.
- Gmail OAuth: the mobile client redirects to `/api/v1/mailboxes/gmail/start`
  in the system browser; the callback returns a deep link back into
  the app with a short-lived one-time code, which the app exchanges at
  `POST /api/v1/mailboxes/gmail/complete`.

`<Mailbox>` DTO:

```json
{
  "id": 42,
  "authType": "IMAP_PASSWORD",  // enum: IMAP_PASSWORD | GOOGLE_OAUTH
  "displayEmail": "person@gmail.com",
  "host": "imap.example.com",     // null for GOOGLE_OAUTH
  "port": 993,                    // null for GOOGLE_OAUTH
  "connectedAt": "2026-06-01T09:12:00Z",
  "lastSyncedAt": "2026-08-28T14:52:03Z",
  "signature": "— Sent from ConexusMail",
  "status": "ACTIVE"              // enum: ACTIVE | NEEDS_RECONNECT | ERROR
}
```

---

## Conversations (TBD)

The mobile client's home is the same texting-style list the web app
shows at `/chats`. Server keys are the participant-set
`conversationKey` (`ConversationKeyService`).

- `GET /api/v1/chats?limit=&cursor=&filter=`
  - `filter`: `ALL` (default), `UNREAD`, `SENT_ONLY`, or a saved-search
    id (`saved:<id>`).
  - `200`: `{ "items": [<ChatListItem>], "nextCursor": string|null }`
- `GET /api/v1/chats/{conversationKey}`
  - `200`: `<Conversation>` — participants, ordered bubble runs,
    thread refs, member metadata.
- `GET /api/v1/chats/{conversationKey}/messages?before=&limit=`
  - Older messages for infinite-scroll paging within a conversation.
- `POST /api/v1/chats/{conversationKey}/send` (idempotent w/ key)
  - Body: `{ "text": string, "attachments": [ ... optional ... ],
    "clientMessageId": "<uuid>" }`
  - `202`: the created message DTO with `status: "SENDING"`. The SSE
    stream will emit a `message-status` event as the SMTP path
    advances the status.
- `POST /api/v1/chats/{conversationKey}/mark-read`
  - `204`.

`<ChatListItem>` DTO (mirrors `service.ConversationListItem`):

```json
{
  "conversationKey": "a1b2c3…",
  "title": "Ada Lovelace",
  "subtitle": "Re: Analytical engine",
  "avatarSeed": "ada@example.com",
  "unreadCount": 2,
  "lastMessageAt": "2026-08-28T14:22:00Z",
  "lastMessagePreview": "That should work — send me the times."
}
```

`<Conversation>` DTO (mirrors `service.ChatConversation`):

```json
{
  "conversationKey": "a1b2c3…",
  "participants": [ <ChatMember> ],
  "runs": [ <BubbleRun> ],
  "threadRefs": [ <ThreadRef> ]
}
```

`<BubbleRun>` and `<ThreadRef>` mirror their `service/` counterparts
and are defined alongside the corresponding Java records when they
gain a JSON mapper.

---

## Live delivery

- `GET /api/v1/events` — Server-Sent Events, `Authorization: Bearer`
  or session cookie. Event kinds:
  - `new-message` — payload `{ conversationKey, messageId }`.
  - `message-status` — payload `{ messageId, status }` (`SENDING`,
    `SENT`, `DELIVERED`, `NOT_DELIVERED`).
  - `chat-updated` — payload `{ conversationKey }` when a chat's
    unread count or last-message preview changes.
- Web Push (roadmap item in the backend CLAUDE.md) is the offline
  path; the mobile client uses OS-native push (APNs / FCM) via a
  parallel `POST /api/v1/push/subscribe` endpoint that stores a device
  token.

---

## Subscriptions & plan (TBD)

- `GET /api/v1/subscription` — current plan, status, renewal date,
  active plan-limit counters (mailboxes used / max, saved searches
  used / max).
- `POST /api/v1/billing/checkout-session` — returns a Stripe Checkout
  URL the app opens in a system browser.
- `POST /api/v1/billing/portal-session` — returns a Billing Portal URL.

Cancel / reactivate flow re-uses the same service methods the web
`/billing/cancel-subscription` page uses.

---

## Team (TBD, Team/Business plans only)

- `GET /api/v1/team` — team members + pending invites.
- `POST /api/v1/team/invites` — invite by email.
- `POST /api/v1/team/invites/{token}/accept` — accept an invite the
  user received.
- `DELETE /api/v1/team/members/{id}` — remove (owner only).

---

## Error catalogue (initial)

| HTTP | `code`                     | When |
| :--- | :------------------------- | :--- |
| 400  | `invalid_request`          | Malformed body / missing required field. |
| 401  | `invalid_credentials`      | Login failed. |
| 401  | `unauthenticated`          | Missing / expired access token. |
| 401  | `invalid_refresh_token`    | Refresh failed; client must re-login. |
| 403  | `email_unverified`         | Account exists but email not verified. |
| 403  | `plan_limit`               | Action would exceed the plan's limit (`field` names the resource, e.g. `mailboxes`). |
| 404  | `not_found`                | Resource doesn't exist for this caller. |
| 409  | `mailbox_already_connected`| Trying to add a mailbox this user already has. |
| 409  | `conflict`                 | Generic optimistic-concurrency conflict. |
| 422  | `validation_failed`        | Field-level validation (populates `field`). |
| 429  | `rate_limited`             | Too many requests; retry after `Retry-After`. |
| 5xx  | `server_error`             | Anything unhandled. |

New codes are added by editing this table in the same commit as the
code that throws them.

---

## Contract review checklist (per run)

Before finishing a run that touched either repo:

- [ ] Every endpoint the mobile client calls appears here.
- [ ] Every JSON DTO field on either side appears in the DTO block
      here, with the same name and type.
- [ ] Every new error `code` appears in the error catalogue.
- [ ] Endpoints not yet implemented carry **Status: TBD**;
      implemented ones name their controller method.
