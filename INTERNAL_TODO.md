# INTERNAL_TODO (severity-ordered)

## Contract breakage
- [ ] No REST API or Bearer/token auth for the mobile client: all functionality is session+CSRF Thymeleaf. Implement `/api/v1` per `docs/API.md` §2 (start: auth token + conversation list/detail).
- [ ] `/chats/events` SSE payload schema is undocumented (see `NewMailEvent`, `ChatEventStream`).

## Logic drift / architecture
- [ ] `ConversationController.send`/`/chats/new` hold business logic (signature append via `withSignature`, reply-vs-new-thread, recipient derivation, sending-account fallback). Extract into a service (e.g. extend `OutboundMessageService`) before adding a REST controller.
- [ ] `ConversationController.chat/chatPane/list` do mailbox-scope selection (`selectMailbox`, `scopeAddress`) in the controller; move to a shared service.

## Missing tests
- [ ] No tests for any `/api/v1` surface (nonexistent). Add MockMvc slice tests alongside the first endpoints.

## Style / housekeeping
- [ ] CLAUDE.md says Dockerfile/docker-compose were removed 2026-06-11 and CI not re-added, but `Dockerfile`, `docker-compose.yml`, `DEPLOY.md` exist in the repo; reconcile.
- [ ] CLAUDE.md "Current status"/commands still say project is not yet scaffolded in places; refresh.
- [ ] Routine prompt says jOOQ; backend is JPA — confirm that is intended.
