# INTERNAL_TODO

Ordered by severity (contract breakage > logic drift > missing tests > style).

## Contract breakage
- No mobile REST API exists; `docs/API.md` lists only 3 machine endpoints. The client has nothing to call.
- No token/bearer auth: SecurityConfig is session-cookie + CSRF only. Mobile login needs a design decision (opaque token table vs JWT).
- `GET /chats/events` SSE payload is undocumented.

## Logic drift / architecture
- Routine assumes jOOQ; backend uses JPA. Decision: keep JPA (do not migrate for the mobile work).
- `ConversationController` holds `selectMailbox`/`scopeAddress`/`chatFor` helpers; extract to a service before a REST controller reuses them so web and API do not fork.

## Missing tests
- None audited this run (tests not executed; see CHANGELOG).

## Branching note
- The scheduled routine says "commit to master"; this session's harness mandates a feature branch (`claude/gifted-carson-zerwoi`). Work was pushed to that branch.
