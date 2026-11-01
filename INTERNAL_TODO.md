# Internal TODO (backend)

Ordered by severity. Created 2026-11-01 (Phase 0 sync audit).

## Contract breakage
1. No `/api/v1` REST layer and no token auth: the Flutter client has nothing
   to call. Needs: contract in `docs/API.md`, bearer auth, CSRF exemption for
   `/api/**`, controllers over existing services.
2. `/chats/events` SSE payload undocumented; `/chats/refresh` relies on session+CSRF.

## Logic drift (web vs future API)
3. `ConversationController` contains mailbox selection/scope helpers
   (`selectMailbox`, `scopeAddress`, `chatFor`, `populateChat`) — extract into a
   service before an API controller needs them, to avoid duplication.

## Spec deviations to resolve
4. Routine prompt assumes jOOQ; code uses JPA/Hibernate. Decide: keep JPA (recommended, no jOOQ codegen exists).
5. Routine prompt says work on `master`; this session was pinned to branch
   `claude/gifted-carson-tujolv` and CLAUDE.md says never push to main. Owner to decide.
6. DEPLOY.md/Dockerfile/docker-compose.yml still exist although CLAUDE.md says Docker was removed.

## Missing tests / tooling
7. `./mvnw test` not run in the Phase 0 session (docs-only change).

## Product backlog
See CLAUDE.md roadmap (support email, OAuth publish, Web Push, delivery states, …).
