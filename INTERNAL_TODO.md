# INTERNAL_TODO

Gaps, tech debt, and deferred work for the full-stack sync routine.
Ordered by severity: contract breakage → logic drift → missing tests →
style.

## Contract breakage (highest priority — mobile app cannot ship without these)

- **[BLOCKER] No REST API exists.** The mobile client needs a
  `/api/v1/**` surface. Today only `POST /chats/refresh` (JSON) and
  `GET /chats/events` (SSE) are usable by a programmatic client. Every
  endpoint under `docs/API.md` "target" section is unimplemented.
  Suggested build order (small commits, one per bullet):
  1. `POST /api/v1/auth/login` + `GET /api/v1/me` — JSON twins of the
     existing form-login. Exempt `/api/v1/**` from CSRF; return session
     cookie.
  2. `GET /api/v1/chats` — wraps `ConversationListService`.
  3. `GET /api/v1/chats/{key}` — wraps `ChatService`.
  4. `POST /api/v1/chats/{key}/messages` — wraps
     `OutboundMessageService`.
  5. Mailbox CRUD + saved-search CRUD.
- **Attachments have no download endpoint.** `Attachment.blob_ref` is
  always null; the 📎 chips in `chat.html` render but nothing serves the
  bytes. Fix is on the roadmap in `CLAUDE.md` under "Per-person shared
  files & links" — add before wiring mobile attachments.

## Logic drift

- `ReplyService` always threads onto the thread's last message. Replying
  to an older bubble mis-threads (breaks `In-Reply-To` semantics). Also
  called out on the `CLAUDE.md` roadmap.
- Sent-folder sync (`last_seen_sent_uid`, V31) is the source-of-truth
  for "message actually delivered" but nothing wires that back into
  bubble status yet. Delivery states on the roadmap.
- The `CLAUDE.md` "Tech stack (target)" line still lists jOOQ, but the
  code uses Spring Data JPA. Reconcile the doc.

## Missing tests

- No integration test for the SSE `new-mail` stream end-to-end. When
  the mobile client subscribes to it, this becomes a shared risk.
- No test coverage for `MailboxPollingService.refreshActiveUserNow`
  returning the imported count that `/chats/refresh` promises.

## Style / cleanup

- `master/` directory (BACKLOG.md, PLAN.md, CHANGELOG.md,
  MASTER_ACTIONS.md) is the project-owner log; consider moving it
  under `docs/master/` so `docs/` is the single-source-of-truth root
  the routine's Phase 0 assumes.
- `INTERNAL_TODO.md` (this file) and `CHANGELOG.md` are new — future
  runs must append, not overwrite.

## Discovered this run

- Phase 0 needed the whole run. No feature was picked or shipped. Next
  run should pick the first bullet under "Contract breakage" (JSON
  login + `/api/v1/me`) and implement it end-to-end on both sides.
- Flutter/Dart toolchain is not installed in the routine's execution
  environment. Mobile-side quality gates (`dart format`,
  `flutter analyze`, `flutter test`) could not run this session and
  will need to run either in CI or on the operator's laptop before
  the app is shipped.
