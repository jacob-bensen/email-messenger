# ConexusMail HTTP API contract

**This file is the canonical contract between the backend (`email-messenger`)
and the mobile client (`conexus-mail-mobile`).** Change it first, then change
both implementations in the same run.

Last audited against code: 2026-11-10.

## 0. Status at a glance

| Area | Status |
|---|---|
| Mobile REST API (`/api/v1/**`, JSON, token auth) | **NOT IMPLEMENTED** — proposed shape in §5 |
| Browser JSON/SSE endpoints used by the web chats page | Implemented — §2 |
| Browser form endpoints (HTML, session cookie + CSRF) | Implemented — §3 |
| Mobile client calls | None (the client repo has no code yet) |

Everything in §1–§4 describes behaviour that exists in code today. §5 is a
design proposal and must not be treated as implemented until its status line
is flipped.

## 1. Conventions (implemented surface)

- **Auth:** server-side session (`JSESSIONID` cookie) established by form
  login (`POST /login`, fields `email`, `password`, optional `remember-me`)
  or Google OIDC (`/oauth2/authorization/google`). A 30-day persistent
  `remember-me` cookie is supported. There is no bearer-token auth.
- **CSRF:** enabled for every state-changing request except
  `POST /billing/webhook` (and the dev-only `/h2-console/**`). Browser forms
  send the `_csrf` field; `fetch` callers send the token in the header named
  by the `_csrf_header` meta tag.
- **Unauthenticated access** to a protected path yields a `302` to `/login`
  (not a `401`) — this is browser behaviour and is one reason a mobile client
  cannot use these endpoints as-is.
- **Timestamps** in the view models are `LocalDateTime` values interpreted as
  UTC; the web templates emit UTC epoch milliseconds.
- **Upload limits:** 10 MB per file, 25 MB per request
  (`spring.servlet.multipart`).

## 2. Machine-readable endpoints (implemented)

### `POST /chats/refresh`
Foreground heartbeat. Synchronously polls the caller's connected mailboxes
and reports how many messages were imported.

- Auth: session + CSRF header.
- Request body: none.
- `200 application/json`:
  ```json
  { "imported": 0 }
  ```
  `imported` — integer ≥ 0, number of newly imported messages.

### `GET /chats/events`
Server-Sent Events stream of live-delivery signals for the signed-in user.

- Auth: session.
- `200 text/event-stream`. Events:
  - `event: new-mail`, `data: {"imported": <int>}` — sent after a poll
    (scheduled, IMAP-IDLE-triggered, or manual) imports ≥ 1 message for the
    user. Delivered after the importing transaction commits.
  - Comment line `: ping` roughly every 25 s as a keep-alive
    (`mailbox.sse.keepalive-ms`).
- The server closes each stream after 30 minutes; clients are expected to
  reconnect (browser `EventSource` does so automatically).

### `GET /chats/{key}/pane`
Returns the open conversation as an **HTML fragment** (timeline, reply box,
signature rail) for in-place swapping. Not JSON. Marks the conversation read.

- Auth: session. Query: optional `mailbox` (mail-account id) to scope.
- `200 text/html` — fragment `chat-pane :: pane`, or `chat-pane :: missing`
  when the key matches no conversation for this user.

### `POST /billing/webhook`
Stripe webhook receiver. Public, CSRF-exempt, authenticated by the
`Stripe-Signature` header. Raw JSON body. Response body is plain text:
`200 ok`; `400` missing or invalid signature; `503` webhook secret not
configured; `500` handler failure.

### `GET /actuator/health`, `GET /actuator/health/**`
Public Spring Boot health endpoint.

### Static/PWA resources
`GET /manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/icon-192.png`,
`/icons/icon-512.png`, `/icons/icon-512-maskable.png`,
`/apple-touch-icon.png`, `/robots.txt`, `/sitemap.xml`,
`/images/og-card.png` — all public.

## 3. Browser form endpoints (implemented)

All return HTML or a redirect. All `POST`s need a CSRF token. "Auth" = signed-in
session required. Page-level detail (templates, model attributes) is in
[`WEB.md`](./WEB.md).

### Chats
| Method | Path | Auth | Parameters | Result |
|---|---|---|---|---|
| GET | `/chats` | yes | `page` (int, default 0; page size 30), `q` (search), `mailbox` (account id), `filter` (`all`\|`unread`\|`awaiting`\|`attachments`, case-insensitive, default `all`) | `chat` view (list, empty pane) |
| GET | `/chats/{key}` | yes | `mailbox` | `chat` view with the conversation open; marks it read; unknown key → redirect `/chats` |
| GET | `/chats/new` | yes | `mailbox` | `compose` view |
| POST | `/chats/new` | yes | multipart: `to` (addresses split on comma/semicolon/whitespace), `subject`, `body`, `attachments[]`, `mailbox` | sends a new email, redirect `/chats/{key}`; validation failure re-renders `compose` |
| POST | `/chats/{key}/send` | yes | multipart: `body` (≤ 100 000 chars), `subject`, `asReply` (bool, default false), `attachments[]`, `mailbox` | sends to the conversation's members, redirect `/chats/{key}` |

Send rules (currently implemented in `ConversationController`):
- A message needs a non-blank `body` **or** at least one non-empty attachment.
- Recipients are always the conversation's non-owner members; the client
  cannot supply them on `/chats/{key}/send`.
- `asReply=true` continues the conversation's most recently active thread
  (keeps its subject and threading headers). Otherwise a new email is sent;
  blank subject becomes `(no subject)`.
- From-address: the `mailbox`-scoped account, else the user's default sending
  account, else the sole connected mailbox, else the login email.
- The sending account's signature (if any) is appended as `\n\n-- \n<signature>`.

### Mailboxes
| Method | Path | Auth | Parameters | Result |
|---|---|---|---|---|
| GET | `/mailboxes` | yes | — | `mailboxes/index` |
| GET | `/mailboxes/new` | yes | `provider` (slug) | `mailboxes/new` |
| POST | `/mailboxes` | yes | `host` (required, ≤ 255), `port` (1–65535, default 993), `ssl` (default true), `username` (required, ≤ 254), `password` (required), `provider` | connect via IMAP password; redirect |
| POST | `/mailboxes/{id}/sync` | yes | — | manual sync; redirect `/mailboxes` |
| POST | `/mailboxes/preferences` | yes | `syncFrequency`, `defaultAccountId` | redirect `/mailboxes` |
| POST | `/mailboxes/{id}/signature` | yes | `signature` (stored ≤ 2000 chars) | redirect `/mailboxes` |
| POST | `/mailboxes/{id}/delete` | yes | — | redirect `/mailboxes` |
| GET | `/mailboxes/gmail/connect` | yes | — | redirect to Google consent |
| GET | `/mailboxes/gmail/reconnect/{id}` | yes | — | redirect to Google consent |
| GET | `/mailboxes/gmail/callback` | yes | `code`, `state`, `error` | redirect |

### Auth & account
| Method | Path | Auth | Parameters |
|---|---|---|---|
| GET | `/login` | no | `plan`, `billing`, `utm_source` |
| POST | `/login` | no | `email`, `password`, `remember-me` (Spring Security form login; throttled by `LoginThrottleFilter`) |
| POST | `/logout` | yes | — |
| GET | `/register` | no | `plan`, `billing`, `utm_source` |
| POST | `/register` | no | `email` (required, valid, ≤ 254), `password` (8–100), `displayName` (≤ 100), `source` (≤ 64), `plan`, `billing` |
| GET | `/auth/google/start` | no | `plan`, `billing`, `utm_source` → redirect `/oauth2/authorization/google` |
| GET / POST | `/password/forgot` | no | POST: `email` |
| GET / POST | `/password/reset` | no | `token`; POST also `password`. Token TTL 1 h |
| GET | `/verify-email` | no | `token`. Token TTL 24 h |
| POST | `/verify-email/resend` | yes | — |
| GET | `/account`, `/account/notifications`, `/account/appearance`, `/account/privacy` | yes | — |
| POST | `/account/appearance` | yes | `theme`, `accent` |
| POST | `/account/password` | yes | `currentPassword`, `newPassword` |
| POST | `/account/email` | yes | `currentPassword`, `newEmail` → redirect `/login?email-changed` |

### Billing, teams, saved searches, admin, misc
| Method | Path | Auth | Parameters |
|---|---|---|---|
| POST | `/billing/checkout` | yes | `plan`, `billing` → redirect to Stripe Checkout |
| POST | `/billing/portal` | yes | → redirect to Stripe portal |
| GET | `/billing/success` | yes | `session_id` |
| GET | `/billing/cancel` | no | → redirect `/pricing?canceled=1` |
| GET / POST | `/billing/cancel-subscription` | yes | POST: `reason` |
| GET / POST | `/team/invite` | yes | POST: `email`. Invite TTL 7 days |
| GET / POST | `/team/invite/accept` | yes | `token` |
| POST | `/searches` | yes | `name`, `q`, `from`, `since`, `unread`, `attachments` |
| POST | `/searches/{id}/delete` | yes | same filter params (used to rebuild the redirect) |
| GET | `/admin/revenue` | admin allowlist (non-admins get 404) | — |
| POST | `/admin/revenue/reconcile-billing-period` | admin | — |
| POST | `/admin/retention/win-back` | admin | `subscriptionId` |
| GET | `/digest/opt-out` | no | `token` |
| GET | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund` | no | `/` and `/pricing` accept `utm_source` |
| GET | `/threads`, `/threads/**`, `/senders` | yes | legacy → redirect `/chats` |

## 4. Errors (implemented surface)

`GlobalExceptionHandler` renders the HTML `error` template for every
controller; there is **no JSON error body** today.

| Status | Cause |
|---|---|
| 400 | `IllegalArgumentException` |
| 404 | unknown route / `NoSuchElementException` (also used to hide `/admin/**`) |
| 409 | `DataIntegrityViolationException` |
| 413 | upload over the multipart limits |
| 502 | mail-server or Stripe failure (`MailException`, `EmailImportException`, `BillingException`) |
| 500 | anything else |
| 302 → `/threads` | `PlanLimitExceededException` (flash attribute `upgradeModal`) |

## 5. PROPOSED mobile API v1 — **not implemented**

Status: **PROPOSED, 0 of 7 endpoints implemented.** Shapes below are derived
from the existing service-layer records (`ConversationListItem`,
`ChatConversation`, `SenderBubbleRun`, `BubbleMessage`, `ChatMember`,
`MailboxView`) so the REST layer can be a thin adapter over the same services
the Thymeleaf pages use. Field names may be adjusted when the first endpoint
ships — update this section in that same change.

Planned conventions:
- Base path `/api/v1`. JSON only. CSRF-exempt; authenticated by
  `Authorization: Bearer <token>`.
- Unauthenticated → `401`, never a redirect.
- Timestamps: ISO-8601 UTC strings (`2026-11-10T14:03:00Z`).
- Error body for every non-2xx:
  ```json
  { "error": "plan_limit_exceeded", "message": "Human-readable text", "details": {} }
  ```
  Planned codes: `unauthorized` (401), `validation_failed` (400),
  `not_found` (404), `conflict` (409), `plan_limit_exceeded` (402),
  `payload_too_large` (413), `login_throttled` (429),
  `upstream_mail_error` (502), `internal_error` (500).

| # | Method | Path | Purpose | Backing service |
|---|---|---|---|---|
| 1 | POST | `/api/v1/auth/token` | exchange `email` + `password` for a bearer token (must go through the same throttle + audit path as form login) | `LoginThrottleService`, `AuthEventService`, new token store |
| 2 | DELETE | `/api/v1/auth/token` | revoke the current token | new token store |
| 3 | GET | `/api/v1/me` | profile, plan, plan limits, connected-mailbox count | `UserService`, `PlanLimitService` |
| 4 | GET | `/api/v1/chats` | conversation list; query `page`, `q`, `mailbox`, `filter` | `ConversationListService` |
| 5 | GET | `/api/v1/chats/{key}` | one conversation timeline; marks read | `ChatService` |
| 6 | POST | `/api/v1/chats/{key}/messages` | send into a conversation (`body`, `subject`, `asReply`, attachments) | send logic currently in `ConversationController` — must be extracted first |
| 7 | GET | `/api/v1/mailboxes` | connected mailboxes and sync state | `MailAccountService` |

Proposed response shapes:

```jsonc
// GET /api/v1/chats
{
  "items": [
    {
      "key": "9f2c…32 hex chars",
      "title": "Ada Lovelace",
      "initials": "AL",
      "group": false,
      "memberCount": 1,
      "preview": "See you Thursday",
      "lastActivity": "2026-11-10T14:03:00Z",
      "unread": true,
      "threadCount": 3,
      "accounts": ["me@example.com"],
      "senderEmail": "ada@example.com"
    }
  ],
  "page": 0,
  "pageSize": 30,
  "totalItems": 124,
  "counts": { "all": 124, "unread": 7, "awaiting": 2, "attachments": 15 }
}

// GET /api/v1/chats/{key}
{
  "key": "…",
  "title": "Ada Lovelace",
  "initials": "AL",
  "group": false,
  "threadCount": 3,
  "members": [
    { "email": "ada@example.com", "displayName": "Ada Lovelace",
      "initials": "AL", "label": "…", "signatureHtml": "" }
  ],
  "runs": [
    {
      "outbound": false,
      "sender": { "email": "ada@example.com", "displayName": "Ada Lovelace", "initials": "AL" },
      "thread": { "threadId": 42, "subject": "Thursday" },
      "messages": [
        { "messageId": 1001, "bodyHtml": "<p>…</p>",
          "sentAt": "2026-11-10T14:03:00Z", "outbound": false,
          "replyToMessageId": null,
          "attachments": [ { "filename": "a.pdf", "mimeType": "application/pdf", "sizeBytes": 1234 } ] }
      ]
    }
  ]
}
```

Open questions to settle when implementing (tracked in `INTERNAL_TODO.md`):
token format and lifetime, how Google-sign-in users obtain a token, whether
`bodyHtml` is delivered as sanitized HTML or a structured/plain form for
native rendering, and push delivery (the SSE stream is session-authenticated).
