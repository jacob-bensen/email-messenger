# INTERNAL_TODO

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
- No mobile REST API or token auth exists; the mobile repo is an empty stub.
  Next feature run: design `/api/v1` auth (API-token table via a new migration,
  stateless filter chain scoped to `/api/**`) + read-only `GET /api/v1/chats`.
- `GET /chats/events` SSE payload is undocumented (`docs/API.md`).

## Logic drift
- Phase 0 audit found no duplicated web/API logic (there is no API yet).

## Missing tests / verification
- Quality gates not run in the Phase 0 doc-only run (see CHANGELOG).

## Style / docs
- Stack deviates from the generic routine template: JPA, not jOOQ.
- Existing CLAUDE.md roadmap items (support email, OAuth consent publish, Web
  Push, delivery states, signature cards, shared files, reply-to-bubble)
  remain open and are tracked there.
