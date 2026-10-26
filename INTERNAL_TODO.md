# INTERNAL_TODO — gaps, debt, deferred work (severity-ordered)

## Contract breakage / blockers
1. **No REST API and no token auth.** Mobile cannot log in or read chats. Implement
   `POST /api/v1/auth/token` + Bearer filter, then `GET /api/v1/chats` (contract draft in
   `docs/API.md` §2). Suggested first feature for the next run.
2. `NewMailEvent` SSE payload shape is undocumented in `API.md` (TBD).

## Logic drift risk (web vs. API sharing one service layer)
3. `ConversationController` privately owns recipient parsing, `sendingAccount`,
   `withSignature`, `selectMailbox`/scope resolution, and attachment conversion. Extract
   into a service so an API controller can reuse it rather than fork it.
4. `ChatService` and `ConversationListService` are package-private in `web`; widen or move
   when an `api` package needs them.

## Missing tests / verification
5. Baseline recorded 2026-10-26: `mvn -B -q test` exit 0, 794 tests, 0 failures, 0 errors, 1 skipped.
6. IMAP IDLE not verified against a real server (from CLAUDE.md roadmap).

## Style / housekeeping
7. Stack differs from generic routine (JPA, not jOOQ) — no codegen step.
8. Existing roadmap lives in `CLAUDE.md`; the routine's "branch = master" instruction
   conflicts with the session branch rule (work on session branch; do not push `main`).
