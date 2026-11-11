# API contract

The canonical contract between the backend (`email-messenger`) and the mobile
client (`conexus-mail-mobile`). Both repos depend on this file; change it
before changing either implementation.

Last audited against the code: 2026-11-11.

## Status

**There is no REST API for the mobile client yet.** The backend is a
server-rendered site with session-cookie auth. Section 1 documents the few
machine-facing endpoints that exist today. Section 2 is a **proposed** v1
contract — nothing in it is implemented, and it must be confirmed and marked
implemented endpoint by endpoint as it ships.

## 1. Implemented endpoints

All of these authenticate with the browser session cookie. State-changing
requests need the CSRF token (`X-CSRF-TOKEN` header, or the header name the
page's `_csrf_header` meta tag gives). A native client cannot use them as-is.

### POST /chats/refresh

Synchronously polls the caller's due mailboxes.

- Auth: session + CSRF.
- Request body: none.
- `200 application/json`:

  ```json
  { "imported": 3 }
  ```

  `imported` is the number of newly imported messages (integer, ≥ 0).
- Unauthenticated: `302` to `/login`. Missing CSRF token: `403`.

### GET /chats/events

Server-Sent Events stream of new-mail signals for the caller.

- Auth: session.
- `200 text/event-stream`. Events:

  ```
  event: new-mail
  data: {"imported":3}
  ```

- A comment line (`: ping`) is sent about every 25 s
  (`mailbox.sse.keepalive-ms`).
- The server closes the stream after 30 minutes; clients reconnect.
- The event carries no conversation identifiers. Clients re-fetch.

### GET /chats/{key}/pane

Returns one conversation as an **HTML fragment** (not JSON) and marks it read.
Optional `mailbox` query parameter (mail account id) scopes the timeline.
Used only by the website.

### POST /billing/webhook

Stripe webhook receiver. Public, CSRF-exempt, authenticated by the
`Stripe-Signature` header. Plain-text responses: `200 ok`,
`400 missing Stripe-Signature header`, `400 invalid signature`,
`503 webhook not configured`, `500 error` (Stripe retries).

### GET /actuator/health

Public liveness probe (Spring Boot Actuator defaults).

### Error format today

`GlobalExceptionHandler` renders an HTML error page for every failure
(404, 400, 409, 413, 502, 500), and a plan-limit failure redirects to
`/threads`. There is no JSON error body anywhere.

## 2. Proposed v1 mobile API — NOT IMPLEMENTED

Everything below is a proposal derived from the existing service layer and
view models. Field names follow the current Java records so the API and the
website can share them.

### Conventions

- Base path `/api/v1`. JSON request and response bodies, `camelCase` fields.
- Timestamps are UTC epoch milliseconds (`number`), matching the
  `*EpochMilli` accessors the website already uses.
- Conversation keys are opaque 32-character hex strings. Clients never
  compute them.
- Auth: `Authorization: Bearer <token>`. Token model (opaque server-stored
  token vs. JWT, lifetime, refresh, revocation) is **TBD** and is the first
  decision to make. `/api/**` would be stateless and CSRF-exempt.
- Errors:

  ```json
  { "error": "plan_limit_exceeded", "message": "Human-readable text" }
  ```

  | HTTP | `error` |
  | --- | --- |
  | 400 | `bad_request`, `validation_failed` |
  | 401 | `unauthorized` |
  | 402 | `plan_limit_exceeded` (adds `kind`, `current`, `limit`) |
  | 404 | `not_found` |
  | 413 | `payload_too_large` |
  | 429 | `too_many_attempts` |
  | 502 | `mail_server_unreachable` |
  | 500 | `internal_error` |

### Endpoints

| Method | Path | Purpose | Backing service today |
| --- | --- | --- | --- |
| POST | `/auth/login` | Exchange email + password for a token | `DbUserDetailsService`, `LoginThrottleService` |
| POST | `/auth/logout` | Revoke the token | TBD |
| GET | `/me` | Account, plan, limits | `UserService`, `BillingService`, `PlanLimits` |
| GET | `/mailboxes` | Connected accounts | `MailAccountService.list` |
| GET | `/conversations` | Paged conversation list | `ConversationListService.list` / `counts` |
| GET | `/conversations/{key}` | Timeline | `ChatService.buildFor` |
| POST | `/conversations/{key}/read` | Mark read | `ChatService.markRead` |
| POST | `/conversations/{key}/messages` | Send into a conversation | logic currently inside `ConversationController.send` |
| POST | `/conversations` | Start a new conversation | logic currently inside `ConversationController.createMessage` |
| POST | `/sync` | Poll now | `MailboxPollingService.refreshActiveUserNow` |
| GET | `/events` | SSE, same events as `/chats/events` | `ChatEventStream` |

### Shapes

`GET /conversations?page=0&q=&filter=all|unread|awaiting|attachments&mailbox={id}`

```json
{
  "items": [
    {
      "key": "9f2c…",
      "title": "Ada Lovelace",
      "initials": "AL",
      "group": false,
      "memberCount": 1,
      "preview": "See you Thursday",
      "lastActivity": 1794370000000,
      "unread": true,
      "threadCount": 2,
      "accounts": ["me@example.com"],
      "senderEmail": "ada@example.com"
    }
  ],
  "page": 0,
  "pageSize": 30,
  "hasNext": false,
  "counts": { "all": 12, "unread": 3, "awaiting": 1, "attachments": 4 }
}
```

`GET /conversations/{key}?mailbox={id}`

```json
{
  "key": "9f2c…",
  "title": "Ada Lovelace",
  "initials": "AL",
  "group": false,
  "threadCount": 2,
  "members": [
    { "email": "ada@example.com", "displayName": "Ada Lovelace",
      "initials": "AL", "label": "Ada Lovelace", "signatureHtml": "" }
  ],
  "runs": [
    {
      "outbound": false,
      "sender": { "email": "ada@example.com", "displayName": "Ada Lovelace",
                  "initials": "AL" },
      "thread": { "threadId": 41, "subject": "Lunch" },
      "messages": [
        { "messageId": 1001, "bodyHtml": "<p>See you Thursday</p>",
          "sentAt": 1794370000000, "outbound": false,
          "replyToMessageId": null,
          "attachments": [
            { "id": 7, "filename": "menu.pdf", "mimeType": "application/pdf",
              "sizeBytes": 48211 }
          ] }
      ]
    }
  ]
}
```

Attachments are metadata only: the backend does not store attachment bytes
yet, so there is no download endpoint to propose.

`bodyHtml` is server-sanitized HTML. Whether the API should also offer plain
text for native rendering is **TBD**.

Request bodies for the send endpoints, the `/me` and `/mailboxes` shapes, and
attachment upload (multipart vs. separate upload) are **TBD**.
