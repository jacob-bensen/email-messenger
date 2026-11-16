# Changelog

Dated entries from the full-stack sync routine, newest first. The earlier
history of this repo is in `master/CHANGELOG.md`.

## 2026-11-16 (c) — Conversation view API (M1)

- New `GET /api/v1/conversations/{key}` (`docs/API.md` §5.6): one
  conversation's members and chronological bubble runs (side, sender,
  thread, messages with `bodyHtml`, a server-derived `bodyText`,
  `replyToId` and attachment metadata). Read-only.
- New `POST /api/v1/conversations/{key}/read` (§5.7): `204`, idempotent.
  Both accept the same `mailboxId` scope as the list; unknown or another
  user's key is `404 not_found`.
- Both go through `web.ChatService` — the same `buildFor` / `markRead` the
  website's `/chats/{key}` uses. `ChatService` is now public; its scoped
  `buildFor` treats a null mailbox as unscoped (so `ConversationController`
  lost its branch), and `markRead` reports whether the conversation exists.
- New `service.BubbleText`: plain text of a bubble's HTML that keeps line
  breaks, for clients that do not render HTML.
- `ApiConversationService`: the user lookup and mailbox-scope resolution
  are shared by list, get and mark read.
- Branching: this run committed to the session branch
  `claude/gifted-carson-wv91t1` (the environment allows pushes only
  there), not `master`; the branch started from `master`'s head.
- Tests: `ApiConversationIntegrationTest` +5 (timeline shape, mark read,
  per-mailbox mark read, foreign/unknown key, auth), `BubbleTextTest` (3).
  Full `./mvnw -B verify`: 830 run, 0 failures, 1 skipped (Docker-only).

## 2026-11-16 (b) — Conversation list API (M1) + branch consolidation

- **Landed on `master`.** Every earlier routine run pushed to its own
  `claude/gifted-carson-*` branch, so `master` never had the docs or
  `/api/v1`. Fast-forwarded `master` to `gifted-carson-saaf8j` (docs, sync
  audit, polling fix, `/api/v1` auth) after `./mvnw verify` passed (815
  tests). From now on the routine commits to `master`.
- New `GET /api/v1/conversations` (`docs/API.md` §5.5): paged
  conversation list with `q`, `mailboxId`, `filter`, per-filter counts.
  `ApiConversationService` / `ApiConversationController` call the same
  `ConversationListService` as `/chats`.
- Moved the filter parsing (`ChatFilter.parse`) and the "scope only with
  more than one mailbox" rule (`ConversationListService.scopeAddress`) out
  of `ConversationController` so web and API share them.
- `/api` errors: a non-numeric query parameter is now
  `400 validation_failed` (was `500`).
- Tests: `ApiConversationIntegrationTest` (7). Full `./mvnw verify`: pass.

## 2026-11-16 — Mobile API foundation + sign in (M1)

- New `/api/v1` for the mobile client (`com.emailmessenger.api`), contract in
  `docs/API.md` §2 and §5: `POST /api/v1/auth/login`,
  `POST /api/v1/auth/logout`, `GET /api/v1/me`.
- Separate stateless `SecurityFilterChain` for `/api/**` (order 1): opaque
  bearer tokens, CSRF off, never a session or cookie, `401` JSON instead of
  the website's login redirect. Website chain unchanged.
- `V36__api_tokens.sql`: per-device tokens, SHA-256 hash only, 30-day
  expiry, cascade-deleted with the user.
- API sign-in reuses the website's `AuthenticationManager`, so audit events,
  `last_login_at` and the 5-in-15-minutes login throttle are shared
  (`429 too_many_attempts` on the API).
- JSON error envelope `{"error":{"code","message"}}` for every API error.
  New `web.RoutingErrorHandler` renders unknown-path / wrong-method errors
  per surface (JSON under `/api/`, HTML elsewhere). Side effect on the
  website: a wrong HTTP method now shows a 405 page instead of a 500.
- `auth.ClientIp` is public so the API can feed the throttle.
- Tests: `ApiAuthIntegrationTest` (15), `ApiTokenServiceTest` (3). Full
  suite: 815 run, 0 failures, 1 skipped (Docker-only end-to-end test).
- Code ported from an earlier, unmerged routine branch
  (`claude/gifted-carson-4weihm`) and reviewed; the contract was rewritten
  against this branch's docs.

## 2026-11-14 — Sync routine bootstrap (Phase 0)

- Added `docs/BACKEND.md`, `docs/API.md` and `docs/WEB.md`,
  reverse-engineered from the code, plus `INTERNAL_TODO.md` with the sync
  audit. Headline finding: the backend has no REST API or token auth, so the
  mobile client has nothing to call yet.
- Fixed mailbox polling outside a caller transaction.
  `MailboxPollingService.pollAll`, `refreshDueForUserAsync` and
  `refreshActiveUserNow` called `pollOne` on `this`, bypassing
  `@Transactional`; the poll then failed on a lazy load and never saved its
  UID cursor, sync time or next-poll time. That covered the 5-minute
  scheduler, the on-open refresh and `POST /chats/refresh` (which always
  returned `{"imported":0}`). `pollOne` now runs in a `TransactionTemplate`.
  New `MailboxPollingNoOuterTransactionTest` fails without the fix.
- `EmailImportService.importMessage(MimeMessage, User)` is now
  `@Transactional` itself instead of relying on the overload it delegates to.
- `ConversationKeyService`: the self-conversation sentinel was a raw NUL
  byte in the source file (tools treated the file as binary); it is now the
  escape `\u0000`. Same hash. Added golden-vector assertions shared with the
  mobile client's Dart mirror.
- `CLAUDE.md`: added "Related repositories" and corrected stale stack notes.
