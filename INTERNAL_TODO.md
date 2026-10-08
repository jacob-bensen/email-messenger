# INTERNAL_TODO (backend)

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
1. No mobile REST API exists. Client has nothing to call. Define `/api/v1` in
   `docs/API.md` first, then add token auth (separate `SecurityFilterChain` for
   `/api/**`, CSRF off, stateless) and thin `@RestController`s over the existing
   services. Needs a decision on token format (opaque DB-backed vs JWT).
2. `GET /chats/events` payload is undocumented; document from `ChatEventStream`.

## Logic drift
3. Chat logic lives partly in controllers (`ConversationController.selectMailbox`,
   `scopeAddress`, `parseFilter`). Move into a service before exposing via REST so
   web and API share it.
4. Task prompt assumes jOOQ; the repo uses JPA. Keep JPA; no codegen. Reconsider only
   if the owner asks.

## Missing tests
5. None audited yet this run (tests not executed — see CHANGELOG).

## Deferred roadmap
See `CLAUDE.md` roadmap (web push, delivery states, contact cards, files, reply-to-bubble).
