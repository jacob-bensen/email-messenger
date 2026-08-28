# API — ConexusMail REST surface (v1)

Canonical contract between the `email-messenger` backend and the
`conexus-mail-mobile` Flutter client. Any change here MUST land in the
same commit — or the same run — as the matching client + server code.

## Base URL

- Local dev: `http://localhost:8080/api/v1`
- Prod: `https://<prod-domain>/api/v1` (domain TBD — see backend
  `master/MASTER_ACTIONS.md`)

## Conventions

- All request and response bodies are `application/json; charset=utf-8`.
- Timestamps are ISO-8601 UTC (`2026-08-28T10:30:00Z`).
- Money values are integer cents.
- Error envelope for every non-2xx response:
  ```json
  {
    "error": {
      "code": "not_authenticated",
      "message": "Human-readable reason."
    }
  }
  ```
- Pagination (when introduced) uses `?cursor=<opaque>` + `?limit=<n<=100>`
  and returns `{ "data": [...], "nextCursor": "..." | null }`. TBD until
  the first paged endpoint lands.

## Authentication (TBD)

The mobile client will authenticate with an opaque bearer token issued by
`POST /api/v1/sessions`. Token storage lives in `mail_account`-style
symmetric encryption on the server. **Not yet implemented** — the only
live endpoint today is `/api/v1/health`, which is public.

Once implemented, protected endpoints require
`Authorization: Bearer <token>` and return `401
{"error":{"code":"not_authenticated"}}` when missing/expired.

## CSRF

`/api/v1/**` is exempt from CSRF (see
`SecurityConfig.securityFilterChain`) because the mobile client does not
carry a session cookie — it uses a bearer token instead.

## Endpoints

### GET /api/v1/health

Public. Liveness probe for the mobile client, distinct from
`/actuator/health` (which the platform uses). Returns 200 with:

```json
{
  "status": "up",
  "serverTime": "2026-08-28T10:30:00Z",
  "apiVersion": "v1"
}
```

The client uses `status == "up"` as the only success signal; extra fields
may appear in future versions and MUST be tolerated by the client parser
(unknown-field-tolerant JSON decoding).

---

## Planned but not implemented (see `INTERNAL_TODO.md`)

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/v1/sessions` | Password login → bearer token. |
| `DELETE` | `/api/v1/sessions/current` | Logout / token revoke. |
| `GET` | `/api/v1/me` | Current user + plan info. |
| `GET` | `/api/v1/mailboxes` | List connected `MailAccount` rows. |
| `GET` | `/api/v1/conversations` | List `EmailThread` rows for the signed-in user, paged, newest-first. |
| `GET` | `/api/v1/conversations/{conversationKey}` | Messages in one conversation. |
| `POST` | `/api/v1/conversations/{conversationKey}/messages` | Send a reply. |
| `GET` | `/api/v1/events` | SSE stream of `NewMailEvent`. |

None of these are wired yet; the shape above is a target for later runs.
