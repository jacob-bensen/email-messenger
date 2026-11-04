# INTERNAL_TODO.md (ordered by severity)

## Contract breakage
- No `/api/v1` REST layer or token auth exists; the mobile client cannot talk to the server. Implement per `docs/API.md` §B (suggested first feature: login + chat list + open chat, read-only).
- Frontend repo contains only a README (no Flutter project yet).

## Logic drift
- Brief says jOOQ; the backend uses JPA. Keep JPA (matches code); update the routine prompt if jOOQ is not intended.
- `ConversationController` builds view models inline (`populateList`, `scopeAddress`, `selectMailbox`); extract to a service before an API controller needs the same mailbox-scope logic.

## Missing tests / unknowns
- Backend baseline verified 2026-11-04: `./mvnw -q -B test` exit 0, 794 tests, 0 failures, 0 errors, 1 skipped.
- `/chats/events` SSE payload shape undocumented (TBD in API.md).

## Process
- Routine prompt says commit to `master`; this session's harness pins feature branches (`claude/*`) and CLAUDE.md says never push to main. Branch used; reconcile the instructions.
- Existing roadmap in CLAUDE.md (support email, Google OAuth publish, Web Push, delivery states, ...) is unchanged.
