# INTERNAL_TODO (backend)

Ordered by severity. Found in Phase 0 sync audit (2026-10-31).

## Contract breakage
1. No JSON API at all; mobile client has nothing to call. Implement `/api/v1` per `docs/API.md` §2 (design first item: stateless token auth chain alongside the session chain in `SecurityConfig`).
2. `GET /chats/events` payload shape undocumented.

## Logic drift
3. Send/list logic lives partly in `ConversationController` (`send`, `populateList`, `sendingAccount`, `withSignature`, attachment conversion). Must be extracted into a service before a REST controller can share it, otherwise logic will fork.
4. `CLAUDE.md` roadmap items remain (support email, Google OAuth publish, Web Push, delivery states, etc.).

## Missing / stale
5. Routine assumes jOOQ; this repo uses JPA — adjust the routine prompt.
6. Attachments are metadata only (`blob_ref` null), so the mobile client can't download them.
7. Routine says work on `master`; session harness assigned branch `claude/gifted-carson-56lvht` and project CLAUDE.md forbids pushing main. Work was done on the assigned branch.
