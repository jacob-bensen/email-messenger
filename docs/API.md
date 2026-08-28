# API.md — Canonical REST contract for the mobile client

**This file is the source of truth.** Both `email-messenger` (backend)
and `conexus-mail-mobile` (Flutter client) MUST match every schema,
path, and error shape defined here. Change this file BEFORE either
implementation.

The Thymeleaf website is a separate transport that reuses the same
service layer (see `docs/WEB.md`); it is not documented here.

## Ground rules

- **Base path:** every REST endpoint lives under `/api/`.
- **Content type:** `application/json; charset=utf-8` for requests and
  responses. Attachments use `multipart/form-data`.
- **Auth:** stateless. Send `Authorization: Bearer <token>` on every
  `/api/**` call except the endpoints explicitly marked "public"
  below. Session cookies are ignored on `/api/**`.
- **Time:** all timestamps are ISO-8601 UTC (`2026-08-28T14:23:11Z`).
  The client renders local time.
- **IDs:** numeric IDs from the database are serialised as JSON
  numbers when they fit safely in a signed 53-bit integer; longer keys
  (conversationKey) are strings.
- **CSRF:** the `/api/**` chain is CSRF-exempt because it is stateless
  and token-authenticated. Browser paths keep CSRF.
- **Error shape:** every 4xx/5xx returns a JSON body:
  ```json
  { "error": "invalid_credentials", "message": "Email or password is incorrect." }
  ```
  `error` is a stable machine-readable slug; `message` is a
  human-readable string suitable for a client toast.

## Versioning

The API is unversioned until 1.0. Breaking changes bump every mobile
client at once — the mobile client refuses to run against a server
whose `/api/version.api` differs (see below).

## Endpoints

### `GET /api/version` — public

Health + build info. The mobile app calls this on launch to prove
reachability and to detect API-incompatible server versions.

**Response 200:**
```json
{
  "app": "conexus-mail",
  "version": "0.1.0-SNAPSHOT",
  "api": 1,
  "serverTime": "2026-08-28T14:23:11Z"
}
```

Fields:
- `app` — stable product slug, always `"conexus-mail"`.
- `version` — Maven `${project.version}` of the running jar.
- `api` — integer contract version. Bumped when a breaking change
  ships. Client compares against its own `SUPPORTED_API` constant and
  refuses to run on mismatch.
- `serverTime` — server clock (UTC), for client drift detection.

No auth required. Never returns 4xx (short of a total outage).

### Planned (not yet implemented — do NOT call from the client until this file promotes them from "planned" to a live section):

- `POST /api/auth/login` — email + password → bearer token.
- `POST /api/auth/logout` — invalidate the current bearer token.
- `GET  /api/me` — the authenticated user's profile + plan.
- `GET  /api/chats?page=0&size=30&filter=all&q=&mailbox=` — paginated
  chat list, matches `ConversationListItem`.
- `GET  /api/chats/{key}` — one conversation's timeline.
- `POST /api/chats/{key}/messages` — send a message (reply-in-thread
  or new-thread mode).
- `POST /api/chats/refresh` — force poll of due mailboxes.
- `GET  /api/mailboxes` — connected mailboxes.

Each endpoint above is added to this file as it ships, with the exact
request/response schemas. Do not implement any of them until this file
promotes them.

## DTO reference

### `VersionResponse`

| field       | type   | notes                                    |
|-------------|--------|------------------------------------------|
| app         | string | always `"conexus-mail"`                  |
| version     | string | Maven project version                    |
| api         | int    | contract version (see Versioning above)  |
| serverTime  | string | ISO-8601 UTC                              |

### `ErrorResponse`

| field   | type   | notes                                       |
|---------|--------|---------------------------------------------|
| error   | string | stable machine slug (`invalid_credentials`) |
| message | string | human-readable, safe to show to end users   |

## Error slugs

| slug                   | HTTP | when                                       |
|------------------------|------|--------------------------------------------|
| `bad_request`          | 400  | payload failed validation                   |
| `invalid_credentials`  | 401  | login failed                                |
| `unauthenticated`      | 401  | missing or invalid bearer token             |
| `forbidden`            | 403  | authenticated but not allowed               |
| `not_found`            | 404  | entity does not exist for this user         |
| `rate_limited`         | 429  | throttled by `LoginThrottleService` et al.  |
| `internal_error`       | 500  | uncaught server error                       |

## Client responsibilities

- On any `401 unauthenticated`, drop the stored bearer token and route
  to login.
- On `api` mismatch from `/api/version`, block the app with an
  "update required" screen.
- Never rely on client-computed values for plan limits, ownership, or
  participant sets — the server recomputes and returns the truth.
