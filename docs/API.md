# API contract

Source of truth for every JSON endpoint that the `conexus-mail-mobile`
client depends on. Both repos MUST match this file. When an implementation
diverges from what's written here, the fix is to either bring the code
back in line OR update this file in the same commit.

## Current state (2026-08-31)

**No JSON API exists yet.** The backend serves HTML through Thymeleaf
controllers for browser-based use only (see [`WEB.md`](./WEB.md)). The
existing SSE stream (`GET /chats/events`) emits text/event-stream frames
that the browser page uses for live refresh — it is not designed for
mobile consumption yet.

The mobile client cannot yet connect. Introducing a `/api/v1/**` surface
is the highest-priority next feature run; see the mobile repo's
`INTERNAL_TODO.md`.

## Conventions (once endpoints exist)

- Base path: `/api/v1`
- Content type: `application/json; charset=utf-8` request and response.
- Auth: token in `Authorization: Bearer <token>`. Token issuance flow TBD
  — the two candidates are (a) an app-password-style personal access
  token minted from the account page, or (b) OAuth2 password grant plus
  refresh. Decision deferred until the first mobile-consumed endpoint is
  written. Sessions cookies are NOT valid on the API surface.
- Timestamps: RFC 3339, always UTC, e.g. `2026-08-31T12:00:00Z`.
- IDs: server-assigned strings. Never trust client-supplied IDs for
  mutations.
- Errors: JSON body
  ```json
  { "error": { "code": "STRING_SLUG", "message": "human string" } }
  ```
  with HTTP status matching the class of failure (400, 401, 403, 404,
  409, 422, 429, 500).
- Pagination: cursor-based. Response includes `nextCursor` (nullable).
  Requests send `?cursor=...&limit=...`, `limit` default 50, max 200.
- Server authority: for every mutation, the server re-reads state,
  validates, mutates, and returns the resulting resource. Never echo
  client-supplied fields the server can compute (timestamps, IDs,
  derived flags).

## Planned endpoints (TBD — none implemented)

The list below is the intended MVP surface for the mobile client. Nothing
here has code yet; each row is a placeholder waiting for a feature run.

### Auth
- `POST /api/v1/auth/token` — exchange credentials (email+password OR
  OAuth code) for a bearer token. Body TBD.
- `POST /api/v1/auth/refresh` — refresh a token. Body TBD.
- `POST /api/v1/auth/logout` — revoke the current token.

### Session
- `GET /api/v1/me` — the authenticated user, their plan, and mail
  accounts summary.

### Mailboxes
- `GET /api/v1/mailboxes` — list connected mail accounts.
- `POST /api/v1/mailboxes/{id}/sync` — trigger a manual sync.
- `DELETE /api/v1/mailboxes/{id}` — disconnect a mail account.

### Chats
- `GET /api/v1/chats` — paginated list of conversations
  (`conversationKey`, participants, last message preview, unread count).
- `GET /api/v1/chats/{key}` — one conversation's bubble timeline.
- `POST /api/v1/chats/{key}/send` — reply into a conversation. Server
  builds the outbound `Message-ID` and threading headers.
- `POST /api/v1/chats/new` — start a new conversation.

### Live delivery
- `GET /api/v1/events` — Server-Sent Events stream, per-user, of
  `new-mail` / `chat-updated` events. Mirrors what `/chats/events` emits
  today but shaped for a mobile consumer.
- Push (FCM/APNs) — TBD, plan-gated.

### Search
- `GET /api/v1/searches` — saved searches.
- `POST /api/v1/searches` / `DELETE /api/v1/searches/{id}` — CRUD.

## Non-goals

- The API is NOT a general-purpose IMAP proxy. It exposes only the
  chat-shaped view of email that ConexusMail computes on the server.
- Billing (Stripe checkout, portal) stays browser-only for now.

## Change log for this file

- 2026-08-31 — Initial version. No endpoints implemented. Establishes
  conventions and the placeholder plan.
