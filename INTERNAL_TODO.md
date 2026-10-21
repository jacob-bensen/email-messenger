# Internal TODO

Ordered by severity (contract breakage > logic drift > missing tests > style).

## Contract breakage
- No mobile API exists: no `/api/v1`, no token auth (session + CSRF only), no JSON error envelope. Client repo is a stub. Next feature run: finalize `docs/API.md` §B and implement auth + `GET /api/v1/chats` + `GET /api/v1/chats/{key}` on top of `ConversationListService` / `ChatService`.
- `/chats/events` SSE event names/payload undocumented (TBD in API.md).

## Logic drift (risk)
- `ConversationController` holds mailbox scoping (`selectMailbox`, `scopeAddress`), filter parsing and attachment validation; extract into a service before a JSON controller needs it, to avoid divergent logic.

## Missing tests / verification
- Backend test suite not run in this routine's discovery pass (docs-only change).
- Flutter SDK absent in this environment: frontend gates (`dart format`, `flutter analyze`, `flutter test`) cannot run.

## Process notes
- The routine prompt says "work on master", but the session harness designates branches `claude/gifted-carson-b88y07` (backend) and `claude/trusting-cori-b88y07` (frontend), and backend CLAUDE.md forbids pushing to main. Harness branches were used.
- `master/` is a tracked directory of planning files (PLAN/BACKLOG/MASTER_ACTIONS/CHANGELOG, a different routine's) — not a git branch; left untouched. Its PLAN.md describes older pricing/naming (MailIM) than CLAUDE.md; reconcile eventually.
