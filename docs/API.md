# ConexusMail HTTP API

**Base URL (dev):** `http://localhost:8080`
**API prefix:** `/api/v1/**`
**API version:** 1
**Content type:** `application/json` unless noted.

This is the canonical wire contract between the ConexusMail backend
(`email-messenger`) and the ConexusMail mobile client
(`conexus-mail-mobile`). Both repos MUST match this file exactly; if a
change breaks the contract, bump `apiVersion` in `/api/v1/version` and
update both implementations in the same commit stream.

The Thymeleaf website is **not** part of this contract — it renders
server-side and does not consume `/api/**`.

---

## Auth model (planned)

The web app authenticates via a session cookie (form login and Google
OIDC — see `SecurityConfig`). The mobile client is **not** designed to
carry that cookie. Once mobile-facing auth ships, requests to
`/api/v1/**` (except the smoke-test endpoint) will require a
`Authorization: Bearer <token>` header issued by a mobile login
endpoint. Both the token model and the login endpoint are TBD; today,
only the unauthenticated smoke-test endpoint exists.

Errors are returned as JSON:

```json
{ "error": "invalid_credentials", "message": "Human-readable explanation." }
```

Status codes follow HTTP semantics: `400` for validation errors, `401`
for missing/invalid auth, `403` for authorization failures, `404` for
missing resources, `409` for conflicts, `429` for rate limit,
`5xx` for server errors.

---

## Endpoints

### `GET /api/v1/version` — smoke test

**Auth:** none.
**Purpose:** confirm reachability and inspect the wire-contract version.

Response `200`:

```json
{
  "service": "conexusmail-backend",
  "version": "0.1.0-SNAPSHOT",
  "apiVersion": 1
}
```

- `service`: fixed string identifying the backend to the client.
- `version`: the backend build's semantic version (from `info.app.version`).
- `apiVersion`: the wire-contract version. Increments on incompatible
  changes anywhere in `/api/**`.

The client MUST refuse to start if `apiVersion` is not in its supported
set — it should point users at an app update instead.

---

## Planned endpoints (TBD — not yet implemented)

Tracked in `INTERNAL_TODO.md` in both repos, ordered by priority:

- `POST /api/v1/auth/login` — email/password login for mobile; returns a
  bearer token + refresh token.
- `POST /api/v1/auth/refresh` — refresh an expired bearer token.
- `POST /api/v1/auth/logout` — revoke the current token.
- `GET  /api/v1/me` — current user profile (id, email, plan, teams).
- `GET  /api/v1/chats` — paginated list of conversations, most-recent first.
  Matches the shape of `ConversationListItem`.
- `GET  /api/v1/chats/{conversationKey}` — one conversation's timeline:
  merged bubbles with quoted-reply stripped, matches `ChatConversation`.
- `POST /api/v1/chats/{conversationKey}/reply` — send a reply (delegates to
  `ReplyService`).
- `GET  /api/v1/events` — server-sent-events stream mirroring
  `/chats/events` (already implemented for the web PWA).
- `GET  /api/v1/mailboxes` — list connected mail accounts.

Every endpoint's DTOs will be defined here BEFORE either implementation
lands. Client and server share this file as their contract.

---

## Change log

- 2026-08-28: Contract created. `apiVersion=1`, only `/api/v1/version`
  implemented.
