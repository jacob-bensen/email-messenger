# API contract

**Status: NOT YET IMPLEMENTED.** This document is the specification the
mobile client (`../conexus-mail-mobile`) and the backend will be held to
once endpoints ship. Nothing in `com.emailmessenger.web` today serves
JSON to a mobile client. The Thymeleaf controllers listed in `docs/WEB.md`
render HTML for logged-in browser sessions and are not part of this
contract.

Base path (proposed): `/api/v1`. All request/response bodies are JSON
(`application/json; charset=utf-8`). Timestamps are RFC 3339 in UTC with
`Z` suffix (e.g. `2026-08-28T14:12:03Z`).

## Auth (proposed)

Session cookies do not fit a native client. The mobile app will use
Bearer tokens:

- `POST /api/v1/auth/login` — body `{ "email", "password" }` → `{ "accessToken", "refreshToken", "expiresIn", "user": UserSelf }`.
- `POST /api/v1/auth/google` — body `{ "idToken" }` (Google Sign-In id token from the device) → same shape as `/auth/login`.
- `POST /api/v1/auth/refresh` — body `{ "refreshToken" }` → `{ "accessToken", "refreshToken", "expiresIn" }`.
- `POST /api/v1/auth/logout` — invalidates the refresh token.
- `GET  /api/v1/me` — returns `UserSelf`.

Access tokens are short-lived (proposed 15 min) JWTs signed with an
HS256 secret held only by the server. Refresh tokens are opaque strings
stored server-side and rotated on every refresh call. Every non-auth
endpoint requires `Authorization: Bearer <accessToken>`; a missing or
expired token returns `401 { "error": "unauthenticated" }`.

## Errors

Non-2xx responses have the shape `{ "error": <slug>, "message": <human>, "details": {...} }`.
Reserved slugs: `unauthenticated`, `forbidden`, `not_found`,
`validation_failed`, `conflict`, `plan_limit_exceeded`, `rate_limited`,
`internal_error`. `plan_limit_exceeded` includes
`details.limitKind` matching `PlanLimitKind` on the server
(`MAILBOX`, `THREAD_HISTORY`, `SAVED_SEARCH`, …).

## Pagination

List endpoints accept `?cursor=<opaque>&limit=<int, ≤100>` and return
`{ "items": [...], "nextCursor": <opaque | null> }`. Cursors are opaque —
the client stores and returns them without parsing.

## Resource shapes (proposed)

```
UserSelf {
  id: string,             // uuid
  email: string,
  displayName: string?,
  plan: "FREE" | "PRO" | "BUSINESS",
  subscription: {
    status: "NONE" | "TRIALING" | "ACTIVE" | "PAST_DUE" | "CANCELED",
    currentPeriodEnd: RFC3339?,
    cancelAtPeriodEnd: bool
  }
}

MailAccountSummary {
  id: string,
  email: string,
  provider: "IMAP" | "GMAIL",
  authType: "PASSWORD" | "OAUTH",
  displayName: string?,
  connectedAt: RFC3339,
  lastSyncAt: RFC3339?,
  needsReconnect: bool     // Gmail: refresh-token expiry
}

ChatSummary {
  conversationKey: string,      // server-computed participant-set hash
  participants: [{ email, displayName?, avatarUrl? }],
  lastMessage: {
    id: string,
    fromEmail: string,
    preview: string,        // stripped + collapsed body, ≤160 chars
    sentAt: RFC3339,
    outbound: bool
  },
  unreadCount: int,
  totalMessages: int
}

ChatDetail extends ChatSummary {
  bubbles: [BubbleRun],
  labels: [string]         // e.g. ["#work", "#invoice"]
}

BubbleRun {
  fromEmail: string,
  displayName: string?,
  outbound: bool,
  messages: [Bubble]
}

Bubble {
  id: string,
  messageId: string,       // RFC 822 Message-ID
  sentAt: RFC3339,
  bodyHtml: string,        // sanitized; safe to render in a webview
  bodyText: string,
  attachments: [{
    id: string,
    filename: string,
    mimeType: string,
    sizeBytes: int,
    downloadUrl: string?   // signed, short-lived; null until blob store lands
  }],
  deliveryState: "SENDING" | "SENT" | "DELIVERED" | "NOT_DELIVERED" | "INBOUND",
  threadRef: string?       // RFC 822 thread badge, if this message belongs to a distinct thread inside the merged conversation
}
```

## Endpoints (proposed)

Mailboxes
- `GET  /api/v1/mailboxes` → `[MailAccountSummary]`.
- `POST /api/v1/mailboxes/imap` — body `{ email, host, port, useSsl, username, password }` → `MailAccountSummary` (server encrypts credentials at rest with `CredentialEncryptor`).
- `POST /api/v1/mailboxes/gmail/start` → `{ authorizeUrl, state }` (the mobile app opens `authorizeUrl` in a browser tab; a deep link into the app carries the resulting `state`+`code` back).
- `POST /api/v1/mailboxes/gmail/finish` — body `{ state, code }` → `MailAccountSummary`.
- `POST /api/v1/mailboxes/{id}/sync` → `{ triggered: bool }`.
- `DELETE /api/v1/mailboxes/{id}`.

Chats
- `GET  /api/v1/chats?cursor=…&filter=UNREAD|ALL|SEARCH:<query>` → paged `[ChatSummary]`.
- `GET  /api/v1/chats/{conversationKey}` → `ChatDetail`.
- `POST /api/v1/chats/{conversationKey}/read` → `{ unreadCount: 0 }` (marks all messages read).
- `POST /api/v1/chats/{conversationKey}/send` — body `{ bodyText, replyToMessageId? }` → `Bubble` (SMTP send from user's connected mailbox; server assigns `deliveryState: SENDING` and streams updates via the events channel below).
- `POST /api/v1/chats/new` — body `{ toEmails: [string], subject?, bodyText }` → `ChatDetail`.

Saved searches
- `GET  /api/v1/saved-searches` → list.
- `POST /api/v1/saved-searches` — body `{ name, query }`.
- `DELETE /api/v1/saved-searches/{id}`.

Live events
- `GET /api/v1/events` — SSE stream. Each event has `event: <type>` and JSON `data`. Types: `new_message` (`{ conversationKey, bubble }`), `chat_read` (`{ conversationKey, readAt }`), `delivery_state` (`{ conversationKey, messageId, deliveryState }`), `mailbox_status` (`{ mailboxId, needsReconnect }`). Reuses the same `NewMailEvent` publisher path as `/chats/events`.

Billing
- `GET  /api/v1/billing/plans` — public, no auth required. Returns the plan config the server enforces (plan slug, price, limits) so the mobile client renders one source of truth.
- `POST /api/v1/billing/checkout` → `{ url }` (Stripe Checkout URL, opened in a browser).
- `POST /api/v1/billing/portal` → `{ url }` (Stripe Customer Portal).

## Contract rules

1. The server is authoritative for all state. The client displays what
   the server returns and never re-derives plan limits, conversation
   keys, or delivery states on its own.
2. Optimistic UI on the client (e.g. an outbound bubble appears
   immediately with `deliveryState: SENDING`) reconciles to the server's
   `delivery_state` event; on any mismatch, the server wins.
3. Every field this document defines is stable. Adding a field is
   backwards-compatible; removing or renaming a field is a breaking
   change and requires a new `/api/v2` path.
4. `docs/API.md` is edited BEFORE code on either side. A PR that changes
   an endpoint without updating this file is incomplete.
