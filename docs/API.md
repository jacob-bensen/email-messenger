# ConexusMail API contract

The canonical contract between this backend (`email-messenger`) and the
mobile client (`conexus-mail-mobile`). Both implementations must match this
file field-for-field. Change it **first**, then both sides, in the same run.

## 1. Overview

- Base path: `/api/v1`. JSON in and out (`Content-Type: application/json`).
- Served by the same Spring Boot app as the website, through its own
  stateless security chain (`api.ApiSecurityConfig`). The website's
  session/cookie/CSRF chain does not apply to `/api/**`, and `/api/**` never
  sets a cookie.
- Timestamps are ISO-8601 UTC instants (`2026-12-15T12:00:00Z`).
- Plans are lowercase strings: `free`, `pro`, `business`.
- Field names are camelCase. A field documented as nullable is always
  present, with `null` when it has no value.

Status (2026-11-15): §5.1 auth and §5.2 `me` are implemented. Conversations,
replies, mailboxes and live updates are not in the API yet — see the mobile
`docs/DESIGN.md` §ROADMAP.

## 2. Authentication

- `POST /api/v1/auth/login` exchanges email + password for an opaque bearer
  token. Every other endpoint needs `Authorization: Bearer <token>`.
- Tokens are 43-character URL-safe random strings, valid for **30 days**
  from sign-in (fixed, not sliding). The server stores only their SHA-256.
- One token per signed-in device; `POST /api/v1/auth/logout` revokes the
  token that made the request and no other.
- A token stops working when it expires, is revoked, or its user is
  disabled. Any of these → `401 unauthorized`; the client must discard the
  token and show sign-in.
- Sign-in uses the website's credential check, audit trail and lockout: a
  successful API login stamps `last_login_at` and writes a `LOGIN_SUCCESS`
  auth event; a failure writes `LOGIN_FAILURE` and counts toward the shared
  lockout (5 failures per email or per IP in 15 minutes).
- Google-only accounts (no password) cannot sign in through the API yet.

## 3. Errors

Every non-2xx response from `/api/**` has this body:

```json
{ "error": { "code": "invalid_credentials", "message": "Incorrect email or password." } }
```

`message` is human-readable and safe to show to the user. Clients branch on
`code`, never on `message`.

| HTTP | `code` | When |
|---|---|---|
| 400 | `validation_failed` | Body missing, not JSON, or fails field rules |
| 401 | `invalid_credentials` | Login: wrong email/password, unknown or disabled account |
| 401 | `unauthorized` | Missing, invalid, expired or revoked bearer token |
| 403 | `forbidden` | Authenticated but not allowed |
| 405 | `method_not_allowed` | Wrong HTTP method for the path |
| 429 | `too_many_attempts` | Login locked by the failure throttle |
| 500 | `internal_error` | Unexpected server error |

Client-only codes (never sent by the server): `network_error` (no HTTP
response), `unexpected_response` (non-2xx without this envelope).

## 4. Shared rules (mirrored by the client)

The server is authoritative. The client keeps pure-Dart mirrors in
`lib/domain/` only to predict results before the server answers.

### 4.1 Conversation key — `service.ConversationKeyService`
Identity of the set of non-owner people in a conversation. Take every
participant email (thread: each sender + To/Cc recipients; Bcc excluded),
trim (Java `String.trim()`: code units ≤ U+0020) and lowercase
(`Locale.ROOT`), drop blanks and the owner's own addresses, de-duplicate,
sort by UTF-16 code unit, join with `\n`, SHA-256 the UTF-8 bytes, and keep
the first 32 hex characters. If no one is left, hash the sentinel
`"\u0000self"` instead.

Golden vectors (owner = `me@example.com`), asserted by both
`ConversationKeyServiceTest` and `test/domain/conversation_key_test.dart`:

| Emails | Key |
|---|---|
| `ada@acme.com` | `154e1e0a1872533ddf397a16e5a65dcf` |
| `␠Bob@Acme.com`, `ada@acme.com`, `ME@example.com`, `ADA@ACME.COM␠` | `487b97fee6493a379888b62e3969cb14` |
| `me@example.com`, `␠␠`, `null` | `95270985d1d4b88724c13887a7794ec3` |

### 4.2 Conversation title and initials — `service.ConversationLabels`
Title: `"You"` for no members, the single label for one, otherwise the first
three labels joined with `", "` plus `" +N"` for the rest. Initials: first
letters of the first two words of the display name (or of the email
local-part when there is no name), uppercased; `"?"` when nothing is usable.

### 4.3 Plan limits — `billing.PlanLimits`
| Plan | mailboxes | threads | savedSearches |
|---|---|---|---|
| free | 3 | 500 | unlimited |
| pro | 5 | unlimited | unlimited |
| business | unlimited | unlimited | unlimited |

The current plan is the subscription's plan when its status is `active` or
`trialing`, otherwise `free`. `GET /api/v1/me` returns the effective limits;
those win over the mirror.

## 5. Endpoints

### 5.1 Auth

#### `POST /api/v1/auth/login` — public

Request:
```json
{ "email": "ada@example.com", "password": "correct-horse", "deviceName": "Pixel 9" }
```
| Field | Type | Rules |
|---|---|---|
| `email` | string | required, not blank; trimmed and lowercased by the server |
| `password` | string | required, not blank |
| `deviceName` | string? | optional, ≤ 100 chars; shown to the user in future device lists |

`200 OK`:
```json
{
  "token": "q8Xv2lN0b3JhZ2UtdG9rZW4tZXhhbXBsZS12YWx1ZS1oZXJl",
  "tokenType": "Bearer",
  "expiresAt": "2026-12-15T12:00:00Z",
  "user": { …Me (§5.2)… }
}
```
Errors: `400 validation_failed`, `401 invalid_credentials`,
`429 too_many_attempts`.

#### `POST /api/v1/auth/logout` — bearer

Revokes the presenting token. `204 No Content`, empty body.
Errors: `401 unauthorized`.

### 5.2 Account

#### `GET /api/v1/me` — bearer

`200 OK` — **Me**:
```json
{
  "id": 42,
  "email": "ada@example.com",
  "displayName": "Ada Lovelace",
  "emailVerified": true,
  "plan": "free",
  "limits": { "mailboxes": 3, "threads": 500, "savedSearches": null }
}
```
| Field | Type | Notes |
|---|---|---|
| `id` | integer | user id |
| `email` | string | sign-in email, lowercase |
| `displayName` | string? | null when the user never set one |
| `emailVerified` | boolean | |
| `plan` | `free` \| `pro` \| `business` | effective plan (§4.3) |
| `limits.mailboxes` / `.threads` / `.savedSearches` | integer? | null = unlimited |

Errors: `401 unauthorized`.

## 6. Relation to the website

The Thymeleaf website (`docs/WEB.md`) and this API must call the same
service-layer classes; controllers only translate HTTP. Today:

| Capability | Website | API | Shared service |
|---|---|---|---|
| Password sign-in | `POST /login` (form) | `POST /api/v1/auth/login` | Spring `AuthenticationManager` + `DbUserDetailsService`, `LoginThrottleService`, `LoginAuditListener` / `AuthFailureListener` |
| Current plan + limits | `/account`, upgrade modal | `GET /api/v1/me` | `PlanLimitService.currentPlan`, `PlanLimits` |
