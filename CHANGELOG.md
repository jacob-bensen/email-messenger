# Changelog

Dated entries from the full-stack sync routine, newest first. The earlier
history of this repo is in `master/CHANGELOG.md`.

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
