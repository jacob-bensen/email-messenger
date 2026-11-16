# Changelog

Dated entries from the full-stack sync routine, newest first. The earlier
history of this repo is in `master/CHANGELOG.md`.

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
