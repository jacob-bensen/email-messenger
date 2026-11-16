# ConexusMail API contract

**Canonical contract between the backend (`email-messenger`) and the mobile
client (`conexus-mail-mobile`).** Both implementations must match this file
exactly. Change it first, then both sides, in the same run.

Last audited against the code: 2026-11-16.

## §1 Status

**There is no JSON API for the mobile client yet.** Every controller is a
Thymeleaf/browser controller: authentication is a session cookie
(`JSESSIONID`, plus a `remember-me` persistent token) obtained by form login
or Google OIDC, and every state-changing request needs a CSRF token. A
native client cannot use any of this today.

The only machine-readable endpoints that exist are listed in §3. They are
browser-facing and are **not** part of the mobile contract.

The first mobile roadmap item (`conexus-mail-mobile/docs/DESIGN.md`
§ROADMAP, M1 "API foundation + sign in") introduces `/api/v1` as proposed in
§2 and §5.

## §2 Conventions (proposed — not implemented)

- Base path `/api/v1`. JSON in and out, UTF-8, `camelCase` field names.
- Timestamps: ISO-8601 with offset (`2026-11-16T09:30:00Z`).
- Auth: `Authorization: Bearer <token>` (§5). No cookies, no CSRF on
  `/api/v1/**`.
- Errors: non-2xx responses carry
  ```json
  { "error": { "code": "PLAN_LIMIT_EXCEEDED", "message": "Human-readable text" } }
  ```
  `code` is a stable SCREAMING_SNAKE identifier the client may switch on;
  `message` is display text. The code list is TBD and will be added here
  with the first endpoint that returns each one.
- Pagination: TBD with the conversation-list endpoint.

## §3 Existing machine-readable endpoints (browser-only)

| Method | Path | Auth | Response |
|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | `{"imported": <int>}` — synchronously polls the user's mailboxes; count of newly imported messages |
| GET | `/chats/events` | session | `text/event-stream`. Event `new-mail` with data `{"imported": <int>}` after any poll imports mail for the user; periodic `: ping` comments keep the stream alive |
| POST | `/billing/webhook` | Stripe signature header (`Stripe-Signature`), no session, CSRF-exempt | Stripe events; 400 on bad signature |

Everything else (`/chats`, `/mailboxes`, `/account`, …) returns HTML or a
redirect; see `docs/WEB.md`.

## §4 Shared rules (mirrored by the client)

The server is authoritative. The client keeps pure-Dart mirrors in
`conexus-mail-mobile/lib/domain/` only to predict or explain a result
before the server answers. A rule here changes only together with the
server class, the mirror, and the shared tests.

### §4.1 Conversation key — `service.ConversationKeyService`

Mirror: `lib/domain/conversation_key.dart` (`conversationKeyFor`).

1. Inputs: the conversation's participant emails (for a thread: every
   message's sender plus To/Cc recipients; **Bcc is excluded**) and the
   owner's own addresses (`email.OwnerAddressService`: the account email plus
   every connected mailbox username).
2. Normalize each email with Java `String.trim()` (strips code units
   `<= U+0020`) then `toLowerCase(Locale.ROOT)`. Drop blanks and nulls.
3. Drop every normalized owner address. De-duplicate.
4. Sort by UTF-16 code unit (Java `TreeSet<String>` natural order), join
   with `\n`. If the set is empty use the sentinel `"\u0000self"`.
5. SHA-256 the UTF-8 bytes; the key is the first 32 lowercase hex chars.

Golden vectors (owner = `me@example.com`), asserted by both
`ConversationKeyServiceTest#goldenVectorsMatchTheMobileMirror` and
`test/domain/conversation_key_test.dart`:

| Emails | Key |
|---|---|
| `ada@acme.com` | `154e1e0a1872533ddf397a16e5a65dcf` |
| `" Bob@Acme.com"`, `ada@acme.com`, `ME@example.com`, `"ADA@ACME.COM "` | `487b97fee6493a379888b62e3969cb14` |
| `me@example.com`, `"  "`, `null` | `95270985d1d4b88724c13887a7794ec3` |

### §4.2 Conversation title and initials — `service.ConversationLabels`

Mirror: `lib/domain/conversation_labels.dart`.

- `title(memberLabels)`: `"You"` when empty; the single label for one
  member; otherwise the first three joined with `", "` plus `" +N"` for the
  rest (`"Ada, Bob, Cy +2"`).
- `initials(displayName, email)`: source is the display name if not blank,
  else the email; an `@`-containing source with no space is cut to its
  local part; split on whitespace; first letter of the first two words,
  uppercased; `"?"` if nothing usable.

### §4.3 Plan limits — `billing.PlanLimits`, `domain.Plan`

Mirror: `lib/domain/plan_limits.dart`. Plans: `FREE`, `PRO`, `BUSINESS`.
"∞" means no enforced cap (`PlanLimits.UNLIMITED` on the server, `null` in
the mirror). Enforced by `billing.PlanLimitService`.

| Plan | Mailboxes | Threads | Saved searches |
|---|---|---|---|
| FREE | 3 | 500 | ∞ |
| PRO | 5 | ∞ | ∞ |
| BUSINESS | ∞ | ∞ | ∞ |

## §5 Mobile authentication (proposal — not implemented)

- `POST /api/v1/auth/login` `{ "email", "password" }` → `200 { "token",
  "expiresAt", "user": { "id", "email", "plan" } }`; `401
  INVALID_CREDENTIALS`; `429 LOGIN_THROTTLED` (reusing
  `auth.LoginThrottleService`).
- `POST /api/v1/auth/logout` revokes the presented token → `204`.
- `GET /api/v1/me` → the `user` object above.
- Tokens: opaque random values, stored server-side as a hash with expiry and
  revocation, so a token can be revoked from the website. A separate
  stateless `SecurityFilterChain` matched on `/api/v1/**` so the website's
  session/CSRF chain is untouched.
- Google sign-in on mobile: TBD (M3).
