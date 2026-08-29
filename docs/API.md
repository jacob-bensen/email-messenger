# API

**Canonical REST contract** between `email-messenger` (backend) and
`conexus-mail-mobile` (Flutter client). Both repos read this file as the
single source of truth for endpoints, request/response shapes, and error
codes.

## Status (2026-08-29)

> The backend currently exposes **no REST API for the mobile client.** All
> user-facing endpoints render Thymeleaf HTML; the only non-HTML endpoint is
> the Stripe webhook (`POST /billing/webhook`), which is not consumable by the
> mobile client.
>
> The mobile client repository is a skeleton with no API calls yet.
>
> This document exists so both repos have a shared place to define the
> contract as it lands. Every planned endpoint below is **TBD** — do not
> implement in the client until the corresponding backend section is filled
> in with schema and the backend has landed.

## Conventions (when endpoints ship)

- Base path: `/api/v1` for the REST surface. Existing Thymeleaf routes keep
  their unversioned paths.
- Content type: `application/json; charset=utf-8` for request and response
  bodies; `application/x-www-form-urlencoded` **only** for the login endpoint
  where the mobile flow reuses the existing Spring Security form-login
  handler.
- Auth: opaque bearer token in the `Authorization: Bearer …` header, minted
  per device. Sessions/cookies remain the mechanism for the Thymeleaf web
  app; the mobile client uses tokens, not cookies, and lives in a distinct
  security filter chain. Introducing the token store is a prerequisite —
  tracked in `INTERNAL_TODO.md`.
- IDs are UUIDs, serialized as strings.
- Timestamps are ISO-8601 UTC with a trailing `Z`.
- Errors: JSON `{ "code": "…", "message": "…", "details": { … } }` with the
  HTTP status. Never leak stack traces.

## Endpoints (all TBD)

### Auth

- `POST /api/v1/auth/login` — email + password → bearer token + refresh token.
  **TBD.**
- `POST /api/v1/auth/google` — Google ID token → bearer token. **TBD.**
- `POST /api/v1/auth/refresh` — refresh token → new bearer + refresh. **TBD.**
- `POST /api/v1/auth/logout` — invalidate the current bearer + refresh. **TBD.**
- `POST /api/v1/auth/register` — email + password → verification email
  triggered. **TBD.**

### Account

- `GET /api/v1/me` — the authenticated user's profile + plan snapshot. **TBD.**

### Mailboxes

- `GET /api/v1/mailboxes` — list mailboxes for the current user. **TBD.**
- `POST /api/v1/mailboxes` — add an IMAP-password mailbox. **TBD.**
- `POST /api/v1/mailboxes/{id}/sync` — request a manual sync. **TBD.**
- `DELETE /api/v1/mailboxes/{id}`. **TBD.**
- Gmail OAuth connect flow — the mobile client hands off to the existing
  server-side OAuth handshake in a browser; token storage is unchanged. **TBD.**

### Conversations (chats)

- `GET /api/v1/chats` — paged list of conversations keyed by
  `conversationKey`, most-recent first. **TBD.**
- `GET /api/v1/chats/{key}` — messages for one conversation, IM-style
  ordering, quoted-reply text stripped by the server. **TBD.**
- `POST /api/v1/chats/{key}/messages` — reply into the conversation. **TBD.**
- `POST /api/v1/chats/new` — start a new conversation from a To/Subject/Body
  form. **TBD.**

### Live delivery

- `GET /api/v1/events` — Server-Sent Events stream of new-message pings for
  the current user's mailboxes, mirroring the existing `/chats/events`
  stream but authenticated with a bearer token. **TBD.**
- Web Push (from the PWA roadmap) is a separate transport and is not part of
  the REST contract.

### Saved searches

- `GET /api/v1/searches`, `POST /api/v1/searches`,
  `DELETE /api/v1/searches/{id}`. **TBD.**

### Signature / contact rail

- `GET /api/v1/contacts/{participantKey}` — signature-derived contact card
  for a participant (blocked on the "Signature → living contact card"
  roadmap item). **TBD.**

## Error codes (planned)

| Code | HTTP | Meaning |
| --- | --- | --- |
| `invalid_credentials` | 401 | Login rejected. |
| `unauthenticated` | 401 | Missing or invalid bearer. |
| `forbidden` | 403 | Authenticated but not allowed (e.g. wrong plan, other user's resource). |
| `not_found` | 404 | Resource does not exist for this user. |
| `validation_failed` | 422 | Request body failed constraints; `details` names fields. |
| `rate_limited` | 429 | Throttle hit; `Retry-After` header set. |
| `plan_limit_hit` | 402 | The action needs a paid plan tier or exceeds a plan cap. |
| `internal_error` | 500 | Unhandled server error; logs carry a correlation id. |

## Non-goals

- Exposing admin (`/admin/**`) endpoints to the mobile client.
- Exposing raw SMTP/IMAP credentials — those remain server-side.
- Duplicating any billing logic client-side — Stripe checkout runs in a
  webview handed off to the existing web flow.
