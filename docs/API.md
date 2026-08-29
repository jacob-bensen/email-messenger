# API contract — `/api/v1`

Canonical contract for the JSON REST API consumed by
`conexus-mail-mobile`. This file is the source of truth: both repos must
match it exactly.

- Base path: `/api/v1`
- Content type: `application/json; charset=utf-8`
- All timestamps: ISO-8601 UTC with `Z` suffix (`2026-08-29T14:07:12Z`)
- Money: integer cents, plus ISO-4217 currency code
- Unknown JSON fields must be ignored by clients (forward-compatible)
- Error shape:
  ```json
  { "error": { "code": "not_found", "message": "human-readable text" } }
  ```

Endpoints without `Auth: required` are public.

---

## `GET /api/v1/health`

Liveness probe used by the mobile client during startup and by uptime
monitors. Always returns 200 while the process is up.

**Response 200**
```json
{ "status": "ok" }
```

---

## `GET /api/v1/version`

Server build identity. The mobile client shows this on the Settings /
About screen and uses it to detect a server upgrade.

**Response 200**
```json
{
  "name": "conexus-mail",
  "apiVersion": "v1",
  "serverVersion": "0.1.0-SNAPSHOT",
  "minSupportedClient": "0.1.0"
}
```

Fields:
- `name` — product identifier, always `"conexus-mail"`.
- `apiVersion` — REST version namespace, `"v1"` today.
- `serverVersion` — Maven `${project.version}` of the running backend.
- `minSupportedClient` — the lowest client SemVer this server accepts;
  clients below this must show a "please update" screen.

---

## `GET /api/v1/plans`

Marketing / pricing surface. Same figures rendered on the `/pricing`
Thymeleaf page.

**Response 200**
```json
{
  "currency": "USD",
  "plans": [
    {
      "id": "FREE",
      "name": "Free",
      "monthlyCents": 0,
      "annualMonthlyCents": 0,
      "limits": { "mailboxes": 3, "threads": 500, "savedSearches": null }
    },
    {
      "id": "PRO",
      "name": "Pro",
      "monthlyCents": 699,
      "annualMonthlyCents": 599,
      "limits": { "mailboxes": 5, "threads": null, "savedSearches": null }
    },
    {
      "id": "BUSINESS",
      "name": "Business",
      "monthlyCents": 0,
      "annualMonthlyCents": 0,
      "limits": { "mailboxes": null, "threads": null, "savedSearches": null }
    }
  ]
}
```

Fields:
- `id` — enum value, matches `com.emailmessenger.domain.Plan`.
- `monthlyCents` — price when billed monthly; `0` for BUSINESS
  (contact-sales) and FREE.
- `annualMonthlyCents` — per-month rate when paid annually; `0` for
  BUSINESS and FREE.
- `limits.*` — the plan cap; `null` means unlimited.

---

## Deferred

The following endpoints are planned and tracked in `INTERNAL_TODO.md`.
They are NOT part of the contract yet — clients must not depend on them:

- `GET /api/v1/me`
- `GET /api/v1/mailboxes`
- `GET /api/v1/chats`
- `GET /api/v1/chats/{conversationKey}/messages`
- `POST /api/v1/chats/{conversationKey}/messages`
- Auth exchange for the mobile client (bearer token flow).
