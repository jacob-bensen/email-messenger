# API — mobile JSON contract

Single source of truth for the JSON API that `conexus-mail-mobile` (Flutter)
will consume. Both server and client MUST match this file exactly; when
either side changes, this file changes in the same commit.

## Status: NOT YET IMPLEMENTED

Today the backend exposes only:

- Thymeleaf HTML pages under paths like `/chats`, `/mailboxes`, `/account`
  — session-cookie authenticated, intended for browsers. See [WEB.md](WEB.md)
  for the full inventory.
- Server-Sent Events at `GET /chats/events` (session-cookie authenticated;
  browser-oriented, uses `EventSource`).
- One true REST endpoint: `POST /billing/webhook`, a Stripe-signed webhook
  receiver (not for clients).

There is no bearer-token auth, no `application/json` chat endpoint, no
mobile-facing DTOs. The mobile repo depends on this file, so this file
is what a client engineer opens first when writing the API — the
absence is itself the top priority.

## Planned base URL and versioning

- Base: `/api/v1`
- Versioning: bump the path prefix on breaking changes; additive changes
  keep the version.
- Content type: `application/json; charset=utf-8` on both directions.
- Timestamps: RFC 3339 with millisecond precision and explicit offset.

## Planned authentication

- `POST /api/v1/auth/login` — email + password, returns an access token
  and refresh token. Password login mirrors the existing form-login rules
  in `LoginThrottleService` (rate limits apply per-IP and per-account).
- `POST /api/v1/auth/refresh` — refresh token → new access token.
- `POST /api/v1/auth/google` — receives a Google ID token from the mobile
  Google Sign-In flow, provisions via `OAuth2ProvisioningService`, returns
  the same token pair.
- All other endpoints: `Authorization: Bearer <access-token>`.

The scheme, token lifetime, storage, and refresh semantics are TBD;
implementation must add a `Filter` or `AuthenticationProvider` alongside
the existing session-based one (see `SecurityConfig`).

## Planned endpoint surface (first cut)

Each row below names a resource that must be added to both this file (with
a full JSON schema) and the backend when it is implemented. The mobile
client MUST NOT ship a call to any endpoint that isn't listed here.

| Method | Path                                | Purpose                                  |
|--------|-------------------------------------|------------------------------------------|
| POST   | `/api/v1/auth/login`                | Password login                           |
| POST   | `/api/v1/auth/google`               | Google ID-token login                    |
| POST   | `/api/v1/auth/refresh`              | Rotate access token                      |
| POST   | `/api/v1/auth/logout`               | Revoke refresh token                     |
| GET    | `/api/v1/me`                        | Current user profile + plan              |
| GET    | `/api/v1/mailboxes`                 | List connected mailboxes                 |
| POST   | `/api/v1/mailboxes`                 | Connect an IMAP-password mailbox         |
| POST   | `/api/v1/mailboxes/{id}/sync`      | Kick a poll                              |
| DELETE | `/api/v1/mailboxes/{id}`            | Disconnect a mailbox                     |
| GET    | `/api/v1/chats`                     | Conversation rail (paged, most recent)   |
| GET    | `/api/v1/chats/{conversationKey}`   | Bubble stream for one conversation       |
| POST   | `/api/v1/chats/{conversationKey}/messages` | Send a reply                     |
| GET    | `/api/v1/chats/events` (SSE or WS)  | Live push of new bubbles                 |
| GET    | `/api/v1/searches`                  | Saved searches                           |
| GET    | `/api/v1/billing/summary`           | Plan + entitlement snapshot              |

None of the above exist yet.

## Planned error shape

All non-2xx responses:

```json
{
  "code": "STRING_MACHINE_CODE",
  "message": "Human string; safe to render",
  "details": { }
}
```

Codes are stable machine identifiers (`AUTH_REQUIRED`, `RATE_LIMITED`,
`PLAN_LIMIT_EXCEEDED`, `NOT_FOUND`, `VALIDATION_FAILED`, …). `details`
is a free-form object per code.

## Contract rules

1. This file changes BEFORE the code on either side.
2. A schema change without a matching bump in both repos in the same run
   is a bug — the sync routine's job is to keep them aligned.
3. Server authority is absolute: any state or number a client sends is
   validated and recomputed; on conflict server wins.
4. Never leak internal identifiers (raw DB IDs) if a stable public key
   exists (use `conversationKey`, `mailAccountId`, etc.).
