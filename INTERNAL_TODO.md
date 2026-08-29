# INTERNAL_TODO.md — backend

Running list of gaps, tech debt and deferred work discovered during the
Phase 0 sync audit. Ordered by severity: contract breakage > logic drift
> missing tests > style.

## Contract / API surface

- **No REST API for mobile client exists yet.** `docs/API.md` defines the
  planned `/api/v1/**` contract. First endpoints to add, in order:
  1. `POST /api/v1/auth/login`, `/auth/refresh`, `/auth/logout`,
     `/auth/google`, `/auth/register`.
  2. `GET /api/v1/me`.
  3. `GET /api/v1/chats`, `GET /api/v1/chats/{key}`.
  4. `POST /api/v1/chats/{key}/send`, `.../mark-read`.
  5. `GET /api/v1/mailboxes`, `POST /api/v1/mailboxes/imap`,
     `POST /api/v1/mailboxes/{id}/sync`.
  6. `GET /api/v1/events` (SSE, bearer-auth).
- **Token store.** Access + refresh tokens need a persistence strategy:
  a new Flyway migration (`V36__api_tokens.sql`), a repository, and a
  token-hashing helper. Refresh tokens rotate on use.
- **Bearer auth filter.** Add a Spring Security filter chain for
  `/api/v1/**` that resolves the bearer token to the same `User` the
  Thymeleaf session uses. Session cookies must not leak into `/api/v1`.

## Documentation

- `docs/BACKEND.md` currently marks the "target stack" jOOQ line from the
  routine prompt as reality-mismatched — the repo uses Spring Data JPA.
  Root `CLAUDE.md` still doesn't call that out; leave it as-is until the
  team confirms the direction, but flag if a jOOQ migration is ever
  considered.
- Weekly regenerate `docs/WEB.md` route table when new controllers land.

## Logic drift risks (to prevent as REST layer is built)

- `ConversationController.chatsRefresh` currently returns ad-hoc JSON
  (`POST /chats/refresh`). When `/api/v1/chats/refresh` lands they MUST
  share the same underlying service call.
- `MessageOutbound` delivery states (Sending/Sent/Delivered/Failed) are
  called for by the roadmap but not implemented; when they land they
  need to flow through both the Thymeleaf morph and the SSE event.
- `ReplyService` always threads onto the thread's last message — this
  will mis-thread mobile reply-to-a-specific-bubble too when it lands.
  Fix in the service, not per-caller.

## Tests

- No test currently covers `/api/v1/**` — none exist yet. Every new REST
  endpoint gets a `@WebMvcTest` slice + an integration test that walks
  the same happy path as the corresponding Thymeleaf test.
- Consider a contract test that loads `docs/API.md`'s JSON schemas and
  asserts controller responses conform. **TBD** whether we go with a
  spec parser or hand-written matchers.

## Deferred (non-blocking)

- Support email (open roadmap item).
- Publish Google OAuth consent screen (open roadmap item; blocked on
  external Google review).
- Web Push (open roadmap item; blocked on VAPID key generation).
- Attachment blob store — required before `GET /api/v1/attachments/{id}/blob`
  can return anything.
- Signature → contact card, per-person shared files, reply-to-bubble —
  all open roadmap items that will each need matching REST endpoints
  once implemented.
