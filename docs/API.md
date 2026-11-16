# ConexusMail API contract

The canonical contract between this backend and the mobile client
(`conexus-mail-mobile`). Both must match it exactly. A change to anything
here ships in the same run as both implementations.

Section numbers are stable. The mobile repo refers to them (§1, §4, §5).

## §1 Status (2026-11-16)

**No mobile API exists yet.** The server is a session-authenticated
Thymeleaf app (`docs/WEB.md`). It has no `/api` routes, token auth or JSON
error format. The first M1 roadmap item in the mobile repo's
`docs/DESIGN.md` builds §2, §3.1 and §5.

The website's own JSON and stream endpoints are **internal**. They are not
part of this contract, and the mobile client must not call them:
- `POST /chats/refresh` returns `{"imported": n}`;
- `GET /chats/events` is an SSE stream that sends `new-mail` with
  `{"imported": n}`;
- `POST /billing/webhook` is Stripe-only.

## §2 Conventions (planned; nothing implemented)

These conventions bind the first API endpoints when they are built.
- **Base path:** `/api/v1`. JSON only (`application/json; charset=utf-8`).
- **Auth:** `Authorization: Bearer <token>` (§5). There are no cookies and no
  CSRF on `/api/**`.
- **Field names and values:**
  - Field names are lowerCamelCase.
  - Timestamps are ISO-8601 UTC strings (`2026-11-16T08:00:00Z`).
  - IDs are JSON numbers, except conversation keys, which are 32-char hex
    strings.
  - Enums are UPPER_SNAKE strings that match the Java enum names
    (`FREE`, `PRO`, `BUSINESS`).
- **Errors:** every non-2xx response has this body:
  ```json
  { "error": { "code": "PLAN_LIMIT_EXCEEDED", "message": "human-readable", "details": {} } }
  ```
  | HTTP | `code` | When |
  |---|---|---|
  | 400 | `VALIDATION_FAILED` | Bad input. `details.fields` maps each field to its message |
  | 401 | `UNAUTHENTICATED` | Token missing, expired or revoked |
  | 401 | `INVALID_CREDENTIALS` | Login failed |
  | 403 | `PLAN_LIMIT_EXCEEDED` | `details` = `{ "kind": "MAILBOX_COUNT" or "THREAD_COUNT" or "SAVED_SEARCH_COUNT", "plan": "FREE", "limit": 3 }` |
  | 404 | `NOT_FOUND` | Missing or not owned. A resource you don't own is reported as missing |
  | 409 | `CONFLICT` | Uniqueness clash |
  | 429 | `RATE_LIMITED` | Login throttle (same rules as the website) |
  | 502 | `UPSTREAM_MAIL_ERROR` | IMAP or SMTP failure |
  | 500 | `INTERNAL` | Anything else |
- **Paging:** cursor-based. A list response is
  `{ "items": [...], "nextCursor": "opaque" | null }`.

## §3 Endpoints

None are implemented. Each subsection becomes normative when its roadmap
item is built. Until then it is a placeholder and both sides must treat
it as TBD.

### §3.1 Auth (planned: M1 "API foundation + sign in")
- `POST /api/v1/auth/login` with `{ "email", "password", "deviceName" }` →
  `200 { "token", "expiresAt", "user": Me }`
- `POST /api/v1/auth/logout` → `204` (revokes the presented token)
- `GET /api/v1/me` → `200 Me`, where
  `Me = { "id", "email", "displayName", "emailVerified": bool, "plan": "FREE"|"PRO"|"BUSINESS", "limits": { "mailboxes": n|null, "threads": n|null, "savedSearches": n|null } }`.
  `null` means unlimited. `limits` comes from `PlanLimits.forPlan(PlanLimitService.currentPlan(user))`.

### §3.2 Conversations (planned: M1): TBD
Paged list rows and the bubble timeline for `/chats/{key}`. The shapes will
be derived from `ConversationListItem` and `ChatConversation`.

### §3.3 Send (planned: M2): TBD
Blocked until compose/reply validation moves out of `ConversationController`.
See `INTERNAL_TODO.md`.

### §3.4 Mailboxes, account (planned: M2): TBD

## §4 Shared rules (mirrored by the client)

The server is authoritative. The client mirrors each rule below in
`lib/domain/` so it can predict a result before the server answers. A rule
here changes only together with both implementations and the shared tests.

### §4.1 Conversation key: `ConversationKeyService`
- **Members.**
  - For a thread: every message's sender plus its TO and CC recipients.
    BCC is never included.
  - For `keyFor`: the given emails.
- **Normalize.** Apply Java `String.trim()` (strips code units ≤ U+0020),
  then `toLowerCase(Locale.ROOT)`. Drop empty results.
- **Exclude the owner.** Drop the owner's addresses, normalized the same
  way. Owner addresses are the user's login email plus every mailbox
  username (`OwnerAddressService`).
- **Join.** De-duplicate and sort by UTF-16 code units (Java `TreeSet`), then
  join with `"\n"`. If nothing is left, use the sentinel
  `"\u0000self"` (a NUL byte followed by `self`).
- **Hash.** SHA-256 over UTF-8; keep the first 32 lowercase hex characters.
- **Golden vectors.** With owner `me@example.com`:

  | Emails | Key |
  |---|---|
  | `ada@acme.com` | `154e1e0a1872533ddf397a16e5a65dcf` |
  | `" Bob@Acme.com"`, `ada@acme.com`, `ME@example.com`, `"ADA@ACME.COM "` | `487b97fee6493a379888b62e3969cb14` |
  | `me@example.com`, `"  "`, `null` | `95270985d1d4b88724c13887a7794ec3` |

  Asserted in both `ConversationKeyServiceTest` (backend) and
  `test/domain/conversation_key_test.dart` (mobile).

### §4.2 Labels: `ConversationLabels`
- **Title.**
  - No members: `"You"`.
  - One member: that label.
  - Otherwise the first 3 labels joined with `", "`, plus `" +N"` when there
    are more.
- **Member labels.** A member's label is the trimmed display name, falling
  back to the email. Members are sorted case-insensitively by label
  (`ConversationListService`).
- **Initials.**
  - The source is the display name if it is not blank, otherwise the email.
  - If the source has no space but has an `@`, use only the part before
    the `@`.
  - Split on whitespace. Use the first letters of the first two parts, or
    one letter if there is only one part, or `"?"`. Uppercase the result.

### §4.3 Plan limits: `PlanLimits`
| Plan | Mailboxes | Threads | Saved searches |
|---|---|---|---|
| FREE | 3 | 500 | unlimited |
| PRO | 5 | unlimited | unlimited |
| BUSINESS | unlimited | unlimited | unlimited |

The effective plan is the subscription's plan when its status is `active` or
`trialing`, and FREE otherwise. Once `/me` exists, the client must prefer the
`limits` it returns over its hard-coded copy.

## §5 Mobile auth (proposal, not implemented)
- **Token issuing.** Issue opaque random bearer tokens (32 bytes,
  base64url). Store only their SHA-256 hash in a new `api_tokens` table:
  `user_id`, `token_hash` (unique), `device_name`, `created_at`,
  `last_used_at`, `expires_at` (90 days, sliding), `revoked_at`. This is the
  same hashed-token pattern as password reset and team invites. JWTs are not
  needed.
- **Security chain.** Add a separate stateless `SecurityFilterChain` ordered
  before the web chain and matching `/api/**`.
  - Bearer auth only, with CSRF off and no session.
  - Unauthenticated requests get 401 with the §2 envelope, not a redirect.
- **Login.** Login checks credentials through the same `AuthenticationManager`
  and `LoginThrottleService` as the website, and records `auth_events`.
- **Google sign-in.** Google sign-in on mobile (M3) exchanges a Google ID token
  for an API token through `OAuth2ProvisioningService`. TBD.
