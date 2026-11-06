# INTERNAL_TODO (backend)

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
- [P0] No REST/JSON API and no token auth exist; the mobile client has nothing to call. Add `/api/v1/**`
  (stateless bearer/JWT or API-token auth, CSRF-exempt, JSON error envelope), documented in `docs/API.md` first.
- [P0] Mobile repo is empty (README only); no Flutter project yet.
- [P1] `GET /chats/events` SSE payload undocumented; confirm shape before client consumes it.

## Logic drift
- Audit result 2026-11-06: no web/API duplication possible yet (only one JSON endpoint). Re-audit when `/api/v1` lands;
  controllers must delegate to `ChatService`/`ConversationListService`/`OutboundMessageService`.
- Controllers `ConversationController` build models inline (`selectMailbox`, `scopeAddress`, `chatFor`) —
  extract to a service before the REST layer reuses them, to avoid copy-paste.

## Missing tests
- Not run this session: `./mvnw test` (docs-only run; only `test-compile` was run).

## Deferred / tech debt
- Docs assume jOOQ in the routine template; repo uses JPA — keep docs honest, no migration planned.
- Existing roadmap lives in `CLAUDE.md`; legacy planning notes in `master/` (CHANGELOG/PLAN/BACKLOG).
- Branching: routine says `master`, session mandated feature branch `claude/gifted-carson-k5x1vg`; used the latter.
