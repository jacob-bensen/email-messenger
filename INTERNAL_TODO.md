# Internal TODO (ordered by severity)

## Contract breakage
- No `/api/v1` exists; mobile client has nothing to call. Auth is cookie+CSRF only.
  **Next feature candidate:** bearer-token auth + read-only `/api/v1/me`, `/chats`,
  `/chats/{key}` (service layer already supports it), then send.
- `/chats/events` SSE payload shape is undocumented (see `fragments/live-refresh.html`).
- Unauthenticated JSON requests get a 302 to `/login`, not a 401.

## Logic drift
- Audit of web-vs-API duplication: N/A until an API exists. Keep controllers thin
  (`ConversationController` is 500 lines; candidate for extracting view-model building into services).

## Docs / tooling
- Stack assumption mismatch: project uses JPA, not jOOQ (documented in BACKEND.md).
- Routine says commit to `master`; this session's branch was `claude/gifted-carson-dbuot0`. Reconcile with owner.
- Roadmap items remain in `CLAUDE.md` (support email, OAuth publish, Web Push, delivery states, ...).
- Baseline: `./mvnw test` = 794 tests, 0 failures, 0 errors, 1 skipped (2026-10-12).
