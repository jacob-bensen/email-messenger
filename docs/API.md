# API — mobile client contract

**Canonical contract** between `email-messenger` (backend) and
`conexus-mail-mobile` (Flutter client). Every request the mobile client
sends and every response the server returns MUST match this file. When
the contract needs to change, edit this file in the SAME commit / run
as the backend and mobile-client changes; never let the three drift.

## Base URL

- Prod (once deployed): `https://<prod-domain>` — hostname TBD (see
  `master/MASTER_ACTIONS.md`).
- Local dev: `http://10.0.2.2:8080` from the Android emulator,
  `http://localhost:8080` from a real device on the same network with
  the port forwarded, iOS simulator uses `http://localhost:8080`.

All paths below are relative to that base URL and are prefixed with
`/api/v1`. Non-`/api` paths belong to the Thymeleaf website and are not
callable from the client.

## Content types

- Requests: `application/json; charset=utf-8` unless noted (multipart for
  attachments — spec'd later).
- Responses: `application/json; charset=utf-8`, UTF-8.
- Dates: ISO-8601 with offset (`2026-08-28T12:34:56Z`).

## Error envelope

Every non-2xx response uses:

```json
{
  "error": {
    "code": "AUTH_REQUIRED",
    "message": "human-readable summary",
    "details": { "field": "email", "reason": "invalid" }
  }
}
```

`code` is a stable machine-readable string (SCREAMING_SNAKE_CASE);
`details` is optional and free-form. Known codes:

| code                    | meaning                                              | typical HTTP |
| ----------------------- | ---------------------------------------------------- | ------------ |
| `AUTH_REQUIRED`         | missing or invalid bearer token                      | 401          |
| `FORBIDDEN`             | authenticated but not allowed                        | 403          |
| `NOT_FOUND`             | addressed resource does not exist                    | 404          |
| `VALIDATION_FAILED`     | request body failed validation                       | 422          |
| `RATE_LIMITED`          | throttled                                            | 429          |
| `INTERNAL`              | server bug — client should retry with backoff        | 500          |

## Auth (planned, NOT YET IMPLEMENTED)

The Flutter client cannot ride the current cookie/form-login session. The
plan below is committed to; endpoints marked **planned** are not yet
serving requests and MUST NOT be called from the client until this doc
drops the marker.

### `POST /api/v1/auth/session`  *(planned)*

Exchange email + password for an opaque bearer token.

Request:

```json
{ "email": "you@example.com", "password": "…" }
```

Response `200`:

```json
{
  "token": "cma_live_…",
  "expiresAt": "2026-09-27T12:34:56Z",
  "user": { "id": 42, "email": "you@example.com", "displayName": "Ada" }
}
```

Errors: `422 VALIDATION_FAILED` (missing fields), `401 AUTH_REQUIRED`
(bad credentials).

### `DELETE /api/v1/auth/session`  *(planned)*

Revoke the current bearer token. `204` on success.

### `GET /api/v1/me`  *(planned)*

Returns the authenticated user + plan summary. `401 AUTH_REQUIRED` if no
valid token.

```json
{
  "id": 42,
  "email": "you@example.com",
  "displayName": "Ada Lovelace",
  "plan": { "code": "PRO", "displayName": "Pro", "renewsAt": "…" }
}
```

## Public endpoints (implemented)

### `GET /api/v1/version`

Returns build metadata; no auth. Lets the client prove it can reach the
API and negotiate future capability flags.

Response `200`:

```json
{
  "app": "conexus-mail",
  "apiVersion": "v1",
  "buildVersion": "0.1.0",
  "serverTime": "2026-08-28T12:34:56Z"
}
```

## Chat endpoints  *(planned)*

Mirrors the `/chats` web workspace. Keyed by `conversationKey`
(participant-set hash) — see `com.emailmessenger.service.ConversationKeyService`.

- `GET /api/v1/chats` — list chats, newest activity first.
- `GET /api/v1/chats/{conversationKey}` — full merged timeline
  (bubbles), signature-rail contact card, thread badges.
- `POST /api/v1/chats/{conversationKey}/replies` — send a reply through
  `OutboundMessageService`.
- `POST /api/v1/chats/{conversationKey}/read` — mark all threads in the
  chat read.
- `GET /api/v1/chats/events` (SSE) — mirror of `/chats/events` for live
  new-mail updates.

Full schemas will be spec'd here when each endpoint is implemented; do
not implement client code against these headings alone.

## Versioning

- The API version lives in the URL (`/api/v1`).
- Additive changes (new endpoint, new field on a response) do not bump
  the version.
- Any breaking change bumps to `/api/v2` and both versions run in
  parallel until the client migration ships.

## Contract-change protocol

1. Edit `docs/API.md` first, in the same commit as the code, on both
   sides (server + client) if both are changing.
2. Server change: `@RestController` under `com.emailmessenger.web.api`,
   thin — delegate everything to a service.
3. Client change: DTO under `lib/data/api/`; call site under
   `lib/data/api_client.dart`; domain rules under `lib/domain/`.
4. Add tests both sides (Spring `@WebMvcTest` on server, Dart test on
   client with a fake HTTP client).
