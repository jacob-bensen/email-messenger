# Changelog

Dated entries from the full-stack sync routine, newest first. The earlier
history of this repo is in `master/CHANGELOG.md`.

## 2026-11-18 — Mailboxes API (M2)

- New `/api/v1/mailboxes` (`docs/API.md` §5.10–5.15): `GET` list (with
  `defaultMailboxId` and the plan's `limit`), `GET …/providers` (the
  website wizard's IMAP presets), `POST` connect with an IMAP password,
  `POST …/{id}/sync`, `DELETE …/{id}`. New error codes
  `403 plan_limit_reached` and `422 mailbox_connect_failed`.
- **One service layer.** `ApiMailboxService` calls the same
  `MailAccountService.list/connect/delete` and
  `MailboxPollingService.pollOne` as `MailboxController`; connect input is
  validated by building the website's `MailboxForm` and running its bean
  constraints, so both paths reject the same input with the same message.
  `MailboxView.errorHint` is now public so the API returns the website's
  remediation hint. `MailboxForm`'s size limits got readable messages
  (website shows them too).
- Tests: `ApiMailboxIntegrationTest` (11: list/cap, connect + encryption,
  validation, 422, 403, unlimited cap, sync ok/failed, owner-scoped
  delete, providers, auth required). Full `mvn -B verify`: 852 tests,
  0 failures, 1 skipped (Docker test).

## 2026-11-17 (b) — Send API: reply and new message (M2)

- **Shared send flow.** New public `web.ChatSendService` owns what was
  private code in `ConversationController`: body/recipient validation,
  recipient parsing, sending-account choice, signature, default subject,
  reply-vs-new. `POST /chats/{key}/send` and `POST /chats/new` now call it,
  as do the new API endpoints, so web and API cannot drift.
  `OutboundMessageService.record*` return the recorded `Message`.
- New `POST /api/v1/conversations/{key}/messages` (`docs/API.md` §5.8) and
  `POST /api/v1/conversations` (§5.9): JSON body, `201
  {key, threadId, messageId}`. New error codes `409 no_recipients` and
  `502 send_failed` (mail server refused; nothing recorded). No
  attachments or idempotency key yet (§6).
- Website behavior unchanged except: an over-long reply now shows "The
  message is too long." (was "Reply body is too long"), and the success
  flash says "Reply sent." only for replies.
- Tests: `ApiSendIntegrationTest` (10), `ConversationControllerTest` +1
  (empty send re-renders with the field error), compose rejection asserts
  the message. Full `mvn -B verify`: 841 tests, 0 failures, 1 skipped
  (Docker test).

## 2026-11-17 — Conversation view API (M1)

- New `GET /api/v1/conversations/{key}` (`docs/API.md` §5.6): one
  conversation's bubble runs (members, runs with sender/thread/outbound,
  messages with UTC `sentAt`, sanitized `bodyHtml`, plain `bodyText`,
  `replyToId`, attachment metadata), optional `mailboxId` scope.
- New `POST /api/v1/conversations/{key}/read` (§5.7): `204`, idempotent,
  `404` when the key has no threads in scope.
- `web.ChatService` is now public and shared by `/chats/{key}` and the
  API; `markRead` returns the number of threads in scope so the API can
  tell "unknown conversation" from "already read". API DTOs are records
  in `ApiDtos`; no JPA entity reaches the wire.
- New `service.HtmlText.toPlainText` (block/`<br>`-aware HTML → text).
- Tests: `ApiConversationIntegrationTest` +4, `HtmlTextTest` (4). Full
  `mvn -B verify`: 830 tests, 0 failures, 1 skipped (Docker test).

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
