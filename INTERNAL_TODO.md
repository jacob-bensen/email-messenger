# INTERNAL_TODO (ordered by severity)

## Contract breakage
- [ ] No mobile API: no `/api/**`, no token auth; the client has nothing to call.
      Next feature run: decide auth (Bearer token), implement `/api/v1/auth/login`
      + `GET /api/v1/chats` + `GET /api/v1/chats/{key}` per `docs/API.md`, add a
      separate stateless `SecurityFilterChain` for `/api/**` (CSRF off there).
- [ ] `/chats/events` (SSE) auth is session-cookie only; decide mobile live-update
      transport (SSE with Bearer vs. push).

## Logic drift / duplication risk
- [ ] `ConversationController` holds orchestration (mailbox scope, recipient
      resolution, signature append, attachment conversion, new-vs-reply in `send`).
      Extract into a service before exposing via REST so web and API share it.
- [ ] `ReplyService` always threads onto the last message (roadmap item in CLAUDE.md).

## Missing tests
- [ ] No tests for any API (none exists). Backend gate not yet run in this
      environment (see CHANGELOG/summary).

## Style / housekeeping
- [ ] Tracked `master/` directory (BACKLOG.md, CHANGELOG.md) in repo root, apparently from an earlier routine; reconcile with root docs or remove (not inspected).
- [ ] Routine assumes jOOQ; project uses JPA. Update the routine prompt.
