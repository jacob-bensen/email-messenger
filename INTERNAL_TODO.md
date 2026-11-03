# INTERNAL_TODO (ordered by severity)

## Contract breakage
- No token/bearer auth: mobile client cannot log in (session+CSRF only). Needs design + `SecurityConfig` API chain.
- No `/api/v1` REST layer; `docs/API.md` planned section is TBD.
- `GET /chats/events` SSE payload undocumented.

## Logic drift
- Generic routine assumes jOOQ; repo uses JPA. Docs reflect JPA; no codegen.
- `conexus-mail-mobile` has no code, so no client rules mirror server rules yet (plan limits, conversationKey, bubble grouping).

## Missing tests / verification
- Maven run not attempted in the Phase 0 run (docs only, offline sandbox has no `~/.m2`).
- Roadmap items in `CLAUDE.md` (Web Push, delivery states, attachments storage, reply-to-bubble).

## Housekeeping
- `master/` directory holds legacy BACKLOG/CHANGELOG/PLAN/MASTER_ACTIONS from an earlier routine; the root `CHANGELOG.md` is the new log. Decide whether to retire `master/`.
- Dockerfile/docker-compose.yml exist at repo root although CLAUDE.md says Docker was removed (2026-06-11).

## Next feature candidate
Bearer-token auth + `GET /api/v1/chats` read-only list, then Flutter login + chat list.
