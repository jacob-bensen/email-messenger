# email-messenger — REST API contract

**Base URL:** deployment-relative, all endpoints under `/api/v1/`.
**Serialization:** JSON via Jackson (default Spring Boot).
**Auth:** none yet — see "Roadmap" below.
**Versioning:** breaking changes bump the `/api/vN/` prefix.

This file is the single source of truth for what the
`conexus-mail-mobile` Flutter client is allowed to expect from the
`email-messenger` backend. Every REST DTO on the Dart side must match a
shape declared here. Update this file **in the same commit** as any
endpoint change.

Backend implementations live in `com.emailmessenger.api`.
Mobile Dart mirrors live in `conexus-mail-mobile/lib/domain/`.

---

## GET /api/v1/plans

Returns the public pricing catalog. Read-only, unauthenticated, safe to
cache client-side for a session.

### Request

No parameters, no body.

### Response — `200 OK`

```json
{
  "plans": [
    {
      "id": "FREE",
      "monthlyCents": 0,
      "annualMonthlyCents": 0,
      "mailboxLimit": 3,
      "threadLimit": 500
    },
    {
      "id": "PRO",
      "monthlyCents": 699,
      "annualMonthlyCents": 599,
      "mailboxLimit": 5
    },
    {
      "id": "BUSINESS",
      "monthlyCents": 0,
      "annualMonthlyCents": 0
    }
  ]
}
```

Field-by-field:

| Field                | Type       | Notes                                                                 |
|----------------------|------------|-----------------------------------------------------------------------|
| `plans`              | array      | Always in enum order: `FREE`, `PRO`, `BUSINESS`.                      |
| `plans[].id`         | string     | Matches `com.emailmessenger.domain.Plan` name.                        |
| `plans[].monthlyCents` | integer  | Month-to-month price, US cents. `0` for `FREE`; `0` for `BUSINESS` (contact-sales). |
| `plans[].annualMonthlyCents` | integer | Per-month rate when paid annually. `0` for `FREE` and `BUSINESS`. |
| `plans[].mailboxLimit` | integer? | Max connected mailboxes. **Absent = unlimited.**                     |
| `plans[].threadLimit` | integer?  | Max imported threads. **Absent = unlimited.**                        |
| `plans[].savedSearchLimit` | integer? | Max saved searches. **Absent = unlimited.**                     |

`FREE` currently returns `mailboxLimit=3`, `threadLimit=500`; `PRO` lifts
`threadLimit` and `savedSearchLimit` (absent); `BUSINESS` is unbounded on
all caps. Numbers come from `com.emailmessenger.billing.PlanLimits` and
`PlanPricing`.

Absent (rather than a sentinel like `-1` or `Long.MAX_VALUE`) is the
"unlimited" wire encoding — mobile parsers translate `null` to their
domain "unlimited" state (see `Plan.mailboxLimit` in
`conexus-mail-mobile/lib/domain/plan.dart`).

### Errors

Read-only endpoint; no expected error responses beyond
transport-level 5xx.

---

## Roadmap — endpoints planned but NOT yet implemented

These are tracked in `INTERNAL_TODO.md` and must be specified here
BEFORE implementation:

- `POST /api/v1/session` — email + password login, returns a session
  token for the mobile app.
- `GET /api/v1/me` — current user + plan.
- `GET /api/v1/me/mailboxes`, `POST /api/v1/me/mailboxes`, …
- `GET /api/v1/me/chats` — paginated conversation rail data.
- `GET /api/v1/me/chats/{key}` — one conversation's bubbles.
- `POST /api/v1/me/chats/{key}/send` — send a reply.

All of the above will require a mobile-friendly auth path that
`SecurityConfig` does not yet expose.
