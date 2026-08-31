# Public JSON API — ConexusMail

Canonical contract between the Spring Boot backend (this repo) and the
Flutter mobile client (`conexus-mail-mobile`). Website Thymeleaf pages
share the same service layer as these endpoints but are **not** part of
this contract — they may render whatever HTML they need.

**Base URL:** `https://<host>` — the mobile client picks it up from a
user-entered server URL and stores it locally.

**Prefix:** every JSON endpoint lives under `/api/v1/`. Breaking changes
bump the prefix; additive changes stay on `/v1`.

**Content type:** requests and responses are `application/json;charset=UTF-8`.
The backend rejects other content types on JSON endpoints.

**Errors:** the shared body is
`{ "error": "code", "message": "human-readable" }` with an HTTP status
matching the class (4xx client / 5xx server). Endpoints below list the
codes they can raise.

**Auth:** none of the endpoints below require auth yet. Session-based
auth for the mobile client is out of scope for run 1 and will be added
alongside the first authenticated endpoint (see `INTERNAL_TODO.md`).

**CSRF:** the whole `/api/v1/**` prefix is exempted from Spring
Security's CSRF filter. Web forms still carry CSRF tokens; the JSON
API is designed to be called by non-browser clients that don't have a
cookie jar.

---

## `GET /api/v1/meta`

Server discovery. The mobile client calls this first to confirm the
entered host is a ConexusMail backend and to learn which auth flows
this deployment supports.

**Auth:** none.
**Rate limit:** none (cheap, cached in-app for a session).

### Response `200 OK`

```json
{
  "apiVersion": "1",
  "serverVersion": "0.1.0-SNAPSHOT",
  "minMobileBuild": 1,
  "serverTime": "2026-08-31T07:27:44.321Z",
  "plans": ["FREE", "PRO", "BUSINESS"],
  "features": {
    "googleOAuth": false
  }
}
```

| Field | Type | Notes |
| --- | --- | --- |
| `apiVersion` | string | Wire version. Currently `"1"`. Mobile refuses to connect to a mismatched major. |
| `serverVersion` | string | Maven project version (from `META-INF/build-info.properties`). Informational; not for gating logic. |
| `minMobileBuild` | int | Lowest `pubspec.yaml` build number the server still accepts. Mobile shows a forced-update wall if its build is lower. |
| `serverTime` | string | ISO-8601 instant in UTC. Client uses this to detect large clock skew. |
| `plans` | string[] | Plan identifiers this deployment recognizes — mirrors `com.emailmessenger.domain.Plan`. |
| `features.googleOAuth` | bool | True iff `auth.google.client-id` and `client-secret` are both set on the server. Drives whether the mobile login screen shows the "Continue with Google" button. |

### Contract stability

Additive-only for `/v1`. New feature flags append to `features`; new
plans append to `plans`; new top-level fields are non-breaking. Any
rename or removal moves the endpoint to `/api/v2/meta`.

---

## Not-yet-defined (see `INTERNAL_TODO.md`)

- `POST /api/v1/auth/login` — email + password → session token.
- `POST /api/v1/auth/logout` — invalidate session token.
- `GET  /api/v1/me` — current user profile + plan.
- `GET  /api/v1/mailboxes` — user's connected mailbox accounts.
- `GET  /api/v1/chats` — paged list of texting-style conversations.
- `GET  /api/v1/chats/{key}` — one conversation's bubble runs.
- `POST /api/v1/chats/{key}/reply` — send an outbound reply.
- `GET  /api/v1/chats/events` — SSE stream mirroring the website's `/chats/events`.

Every one of these must land in the same run as its mobile-side call
site — the routine forbids contract drift.
