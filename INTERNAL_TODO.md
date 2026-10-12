# INTERNAL_TODO (backend)

Sync audit 2026-10-12, ordered by severity.

## Contract breakage
1. **No mobile REST API exists.** Frontend repo (`conexus-mail-mobile`) is empty;
   `docs/API.md` §2 is a draft. Needs: bearer-token auth (stateless filter chain
   for `/api/**`, CSRF-exempt), JSON controllers, error envelope. Pick first
   slice: login + chat list + open chat + send.

## Logic drift (web vs. API)
2. `ConversationController` holds send orchestration (recipient parsing, sender
   selection `sendingAccount`, `withSignature`, attachment conversion,
   `scopeAddress`/`selectMailbox`). Extract into a service before adding JSON
   endpoints so both paths share it.

## Docs / process
3. Routine prompt assumes jOOQ + `master`; repo is JPA and sessions here are
   pinned to `claude/*` branches. Docs written for reality; confirm preferred branch policy.
4. `master/` (dir) holds the long-form legacy `CHANGELOG.md`, `BACKLOG.md`,
   `PLAN.md`, `MASTER_ACTIONS.md`. Root `CHANGELOG.md` is the routine's log;
   decide whether to merge them. (Note `git log master` is ambiguous with this dir.)
5. `GET /chats/events` event payload undocumented.

## Missing tests
6. `mvn -o compile` failed on 2026-10-12 (parent POM not in offline cache; sandbox has no cached deps). Docs-only run, tests NOT run.
