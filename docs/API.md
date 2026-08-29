# ConexusMail API

Canonical contract between the Spring Boot backend and the
`conexus-mail-mobile` Flutter client. Both repos MUST match this file — any
change to a request or response shape must land here in the same commit as
the code changes.

**Base URL.** All mobile endpoints live under `/api/*`. Endpoints served
outside this prefix (Thymeleaf pages under `/`, `/actuator/*`, Stripe
webhook at `/billing/webhook`) are NOT part of the mobile contract.

**Content type.** Requests and responses are `application/json;
charset=utf-8`.

**Auth.** The mobile client uses the same session cookie as the website
(`JSESSIONID`), obtained through form-login. `/api/**` is exempt from CSRF
because the mobile client cannot participate in the browser CSRF-token
flow; endpoints that mutate state MUST gate themselves with an explicit
auth check on the controller. Only `/api/health` is anonymous today.

## Conventions

- **Timestamps.** ISO-8601 UTC (`2026-08-29T09:12:00Z`).
- **Errors.** `4xx`/`5xx` return a JSON body of the shape
  `{"error": "code", "message": "human readable"}`. `401` from a
  protected endpoint means "log in again."
- **Versioning.** `/api/health.version` names the deployed server build.
  When the client and server disagree on the contract, the client surfaces
  a "please update" banner rather than crashing.

## Endpoints

### GET `/api/health`

Anonymous liveness + version probe. The mobile splash screen calls this
before showing the login form; if it fails, the client shows an offline
banner and retries with backoff.

Response `200 OK`:

```json
{
  "status": "ok",
  "version": "0.1.0-SNAPSHOT",
  "timestamp": "2026-08-29T09:12:00Z"
}
```

- `status`: always `"ok"` when the server can respond.
- `version`: the server's build version. Sourced from Spring Boot
  `BuildProperties` when available, otherwise `"dev"`.
- `timestamp`: server-side clock at the moment of the response, ISO-8601
  UTC.

## Roadmap (next endpoints)

Ordered by the mobile client's dependency chain — do NOT reorder without
updating `../conexus-mail-mobile/docs/DESIGN.md` roadmap in the same
commit.

1. `POST /api/auth/login` — form-login-equivalent JSON handshake; returns
   the session cookie and a `User` payload.
2. `GET /api/auth/me` — current session's user; `401` if unauthenticated.
3. `POST /api/auth/logout` — invalidate the session.
4. `GET /api/mailboxes` — list connected mail accounts for the current
   user (mirrors `MailboxController`'s data).
5. `GET /api/chats` — the participant-set-keyed conversation list, paged
   (mirrors `ConversationListService`).
6. `GET /api/chats/{key}` — a single conversation's messages.
7. `POST /api/chats/{key}/reply` — send a reply through the existing
   `ReplyService`.

None of the endpoints in the "next" list are implemented yet. Update this
file as each ships and remove the item from the list.
