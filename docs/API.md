# API.md — canonical REST contract between backend and mobile client

This is the shared source of truth between `email-messenger` (server)
and `conexus-mail-mobile` (Flutter client). Every endpoint the mobile
app calls MUST be documented here first, then implemented on both sides
in the same run. If the code and this file disagree, this file is
wrong — but it must be corrected in the same commit that fixes the
drift.

## Status: **no REST API implemented yet**

As of 2026-08-30 the backend serves only Thymeleaf pages (see
`docs/WEB.md`) and a couple of same-origin SSE / form-post endpoints
used by the web UI. There is **no `/api/v1/*` surface, no JSON DTOs,
and no bearer-token auth**. Everything below is the initial plan.

The Flutter client cannot ship a real feature until the corresponding
REST endpoint exists. The bootstrap order the sync routine will follow:

1. **Auth for mobile.** Add `/api/v1/auth/login` (email + password →
   session token) and `/api/v1/auth/google` (Google ID token →
   session token). Persist tokens in a new `mobile_session` table
   (opaque token, user_id, expires_at, revoked_at). Add a
   `MobileAuthFilter` that reads `Authorization: Bearer …` and
   populates the same `Authentication` principal the web login uses,
   so downstream services stay unchanged.
2. **Chats list.** `GET /api/v1/chats` returning the same
   participant-set-keyed rows the `/chats` page renders.
3. **Chat pane.** `GET /api/v1/chats/{conversationKey}` returning the
   IM-transformed message list.
4. **Send.** `POST /api/v1/chats/{conversationKey}/messages`.
5. **Live delivery.** `GET /api/v1/events` (SSE, mirrors the web
   `/chats/events`).

Everything past that is prioritized by
`conexus-mail-mobile/docs/DESIGN.md`.

## Conventions (once we start shipping endpoints)

- Base path: `/api/v1`. Never break v1 without bumping to `/api/v2`.
- Content type: `application/json; charset=utf-8` in and out.
- Auth: `Authorization: Bearer <opaque-session-token>`. No JWTs — the
  server owns session lifecycle so it can revoke instantly.
- Timestamps: RFC 3339 with `Z` suffix (UTC), never local time.
- IDs: opaque strings unless the entity already uses a stable numeric
  ID in the DB — then send it as a string in JSON to keep the mobile
  side language-agnostic.
- Pagination: cursor-based. Response envelope
  `{ "items": [...], "nextCursor": "..." | null }`. Never
  offset/limit — the underlying tables are mutable.
- Errors: HTTP status + JSON body
  `{ "error": "<code>", "message": "<human-readable>", "details": {…}? }`
  where `<code>` is a short kebab-case slug (e.g. `unauthorized`,
  `plan-limit-reached`, `mailbox-not-found`).
- Validation errors: `422` with `details.fields` = map of field →
  message.
- All state-mutating requests are idempotent-safe with an
  `Idempotency-Key` header (client-generated UUID). Server stores the
  key + response for 24h and replays on retry.
- CSRF is disabled ONLY under `/api/v1/**` (bearer-token auth removes
  the need). The Thymeleaf site keeps CSRF on.

## Endpoint index

_(populate per endpoint as they land — one H3 per route, with request
schema, response schema, error codes, and example)_

### Auth

_TBD — see status section above._

### Chats

_TBD._

### Mailboxes

_TBD._

### Billing

_TBD (mostly server-driven; likely just `GET /api/v1/me/subscription`
and a "open Stripe portal" deep link)._

## Deprecation policy

Any field or endpoint marked deprecated stays live for **90 days**
before removal, and both repos must update in lockstep before that
window closes. Removing an endpoint is a contract change — same rules
as adding one (docs first, both sides same run).
