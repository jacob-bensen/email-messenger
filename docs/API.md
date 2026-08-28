# API

The canonical contract between the Spring Boot backend
(`email-messenger`) and the mobile client (`conexus-mail-mobile`).
Both repositories depend on this file — if you change an endpoint's
shape, update it here **first**, then implement on both sides in the
same run.

## Status: no mobile-facing REST API exists yet

Today the backend serves:

- The Thymeleaf website + authenticated `/chats` workspace
  (see `docs/WEB.md`).
- One SSE stream: `GET /chats/events` (text/event-stream) — used by the
  in-browser chats page to receive `NewMailEvent`s in real time.
- One inbound webhook: `POST /billing/webhook` — Stripe events.

Everything else is HTML-returning `@Controller`s driving Thymeleaf
templates. `ConversationController#pane` (`GET /chats/{key}/pane`)
returns an HTML fragment for morph-in updates, not JSON.

**There is no JSON REST surface a native mobile client can consume, and
no bearer-token auth layer.** Building that out is the first big
cross-repo item — captured in `INTERNAL_TODO.md` (this repo) and in
`docs/DESIGN.md` §ROADMAP (mobile repo).

## Target contract (planned — nothing implemented yet)

When we build the mobile API, the shape below is the starting point.
Treat it as design intent, not the current state.

### Base

- Base URL: `https://<prod-domain>/api/v1`
- Content type: `application/json; charset=utf-8`
- Auth: `Authorization: Bearer <access-token>` on every request except
  the sign-in endpoints. Access tokens are short-lived (~15 min); pair
  with a refresh token stored in the client's secure storage.
- Time fields: RFC 3339 in UTC (`2026-08-28T12:34:56Z`).
- IDs: server-issued `int64` values, serialized as JSON numbers.
- Errors: uniform envelope
  ```json
  {"error": {"code": "unauthenticated", "message": "..."}}
  ```
  Codes: `unauthenticated`, `forbidden`, `not_found`, `conflict`,
  `validation_failed`, `rate_limited`, `internal`.

### Auth (planned)

| Method | Path                       | Purpose                                    |
|--------|----------------------------|--------------------------------------------|
| POST   | `/api/v1/auth/login`       | Email + password → access + refresh tokens |
| POST   | `/api/v1/auth/google`      | Google OAuth id-token exchange             |
| POST   | `/api/v1/auth/refresh`     | Trade refresh token for a new access token |
| POST   | `/api/v1/auth/logout`      | Revoke the refresh token                   |
| GET    | `/api/v1/me`               | Current user + plan + verification state   |

### Mailboxes (planned)

| Method | Path                                | Purpose                                    |
|--------|-------------------------------------|--------------------------------------------|
| GET    | `/api/v1/mailboxes`                 | List connected mailboxes                   |
| POST   | `/api/v1/mailboxes`                 | Connect an IMAP mailbox (password auth)    |
| POST   | `/api/v1/mailboxes/gmail/start`     | Kick off Gmail OAuth on-device             |
| POST   | `/api/v1/mailboxes/{id}/sync`       | Force an immediate poll                    |
| DELETE | `/api/v1/mailboxes/{id}`            | Disconnect                                 |

### Conversations & messages (planned)

| Method | Path                                       | Purpose                                          |
|--------|--------------------------------------------|--------------------------------------------------|
| GET    | `/api/v1/conversations?cursor=...`         | Paginated conversation list (most-recent first)  |
| GET    | `/api/v1/conversations/{key}`              | One conversation: participants + message page    |
| GET    | `/api/v1/conversations/{key}/messages`     | Older-message pagination                         |
| POST   | `/api/v1/conversations/{key}/messages`     | Send a reply (subject, body, attachments)        |
| GET    | `/api/v1/attachments/{id}`                 | Download attachment bytes (see gap below)        |

Conversation objects mirror `com.emailmessenger.service.ChatConversation`:
`key` (string), `members` (list of `{email, displayName, avatarUrl?}`),
`lastMessage` (subject, preview, sentAt, direction), `unreadCount`.

Message objects mirror `com.emailmessenger.domain.Message`:
`id`, `messageIdHeader`, `inReplyTo`, `sender`, `recipients`, `subject`,
`bodyPlain`, `bodyHtml`, `sentAt`, `direction` (`inbound`/`outbound`),
`deliveryStatus` (see roadmap item), `attachments`.

### Realtime (planned)

Reuse the existing SSE stream shape: `GET /api/v1/events` returns
`text/event-stream`; each event has `type` (`new-message`,
`conversation-updated`, `mailbox-status`) + a JSON `data` payload
matching the entity above.

### Saved searches, teams, billing

Out of scope for the initial mobile MVP. Web-only until the core chat
flow is proven on device.

## Known gaps

- **No JSON REST controllers exist.** Every endpoint above is unbuilt.
- **No bearer-token auth.** Only session cookies + Google OIDC via the
  browser. A mobile client needs opaque refresh tokens (or JWTs) plus a
  device-registration flow.
- **Attachment downloads.** `Attachment.blob_ref` is always null today —
  the bytes aren't stored, so `GET /api/v1/attachments/{id}` cannot
  work yet. The roadmap item "Per-person shared files & links" in
  backend `CLAUDE.md` covers standing up a blob store.
- **Delivery states.** Outbound `Message` doesn't carry a delivery
  status yet. Roadmap item "Delivery states for outbound bubbles"
  needs to land before the mobile client can render iMessage-style
  Sending / Sent / Delivered / Not Delivered.
- **Push notifications.** The backend PWA path uses Web Push
  (VAPID keys, not implemented yet). Mobile will need APNs + FCM
  through the same push-subscription abstraction — extend that layer
  when Web Push lands.

Update this file the moment any of these gaps starts closing.
