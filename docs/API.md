# API contract

**Canonical contract between the backend (`email-messenger`) and the mobile
client (`conexus-mail-mobile`).** Change this file first, then both
implementations, in the same run.

Last audited against code: 2026-11-11.

## 1. Status

**No mobile REST API exists yet.** The backend is server-rendered
(Thymeleaf). The mobile repo contains no client code. Section 2 documents the
machine-readable endpoints that *do* exist; Section 3 lists what is planned
and is explicitly **not implemented**.

## 2. Implemented endpoints

All of these authenticate with the browser session cookie. State-changing
requests need the Spring Security CSRF token (header name and value are
rendered into `<meta name="_csrf_header">` / `<meta name="_csrf">`).
An unauthenticated request is redirected (302) to `/login` — there is no
JSON 401.

### POST `/chats/refresh`

Synchronously polls the caller's connected mailboxes.

- Auth: session + CSRF.
- Request body: none.
- `200 application/json`:
  ```json
  { "imported": 0 }
  ```
  `imported` (integer) — number of newly imported messages.

### GET `/chats/events`

Server-Sent Events stream of new-mail signals for the caller.

- Auth: session.
- `200 text/event-stream`. Server-side timeout 30 minutes; clients reconnect.
- Event `new-mail`, data:
  ```json
  { "imported": 3 }
  ```
- Comment-only keepalive (`: ping`) roughly every 25 s
  (`mailbox.sse.keepalive-ms`).

The event carries no conversation identifiers; the web client reacts by
re-fetching the HTML rail/pane.

### POST `/billing/webhook`

Stripe → server only; not for clients. Requires a valid `Stripe-Signature`
header. Plain-text responses: `200 ok`, `400 missing Stripe-Signature header`,
`400 invalid signature`, `503 webhook not configured`, `500 error`.

### GET `/actuator/health`

Public. Standard Spring Boot health JSON (`{"status":"UP"}`).

### HTML fragment endpoint (not JSON)

`GET /chats/{key}/pane` returns an HTML fragment of one conversation and
marks it read. Listed here only because it is fetched by script; it is not
part of the mobile contract.

### Errors

Every error path renders the HTML `error` template (statuses 400, 404, 409,
413, 502, 500 — see `GlobalExceptionHandler`). **There is no JSON error
format.**

## 3. Planned mobile API — NOT IMPLEMENTED

Nothing below exists. Shapes are **TBD** until the run that implements each
item fixes them here first. Listed so both repos share one plan
(see `conexus-mail-mobile/docs/DESIGN.md` §ROADMAP).

Open decisions that block the first endpoint:

- **Auth mechanism — TBD.** Session cookies + CSRF are a poor fit for a
  native client. Needs a token scheme (issue, storage hashed at rest,
  revoke, expiry) and its interaction with login throttling, email
  verification and Google sign-in.
- **Namespace/versioning — TBD** (working assumption: `/api/v1/**`).
- **JSON error envelope — TBD** (the existing handler is HTML-only).

Intended first slice, in order:

1. Auth: obtain / revoke a token; `GET` current user (email, display name,
   plan, email-verified).
2. Conversation list — would expose `ConversationListItem` (key, title,
   initials, group, memberCount, preview, lastActivity, unread, threadCount,
   accounts, senderEmail) with paging, `q`, `mailbox`, and filter
   `ALL | UNREAD | AWAITING | ATTACHMENTS`, plus `ChatCounts`.
3. Conversation detail — would expose `ChatConversation` (members, runs of
   `BubbleMessage`); opening marks read.
4. Send into a conversation / start a new one.
5. Mailboxes: list, connect (IMAP), sync, delete.
6. Live updates: reuse `/chats/events` or push — TBD.
