# INTERNAL_TODO (backend)

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
1. No token-based auth and no `/api/v1` REST layer; the mobile client cannot talk to the
   server (session+CSRF form login only). Design in `docs/API.md` §B. Blocks all client work.
2. `GET /chats/events` SSE payload is undocumented (read `ChatEventStream`) — pin it down.

## Logic drift / architecture
3. Target stack says jOOQ; repo uses Spring Data JPA. Decision needed: keep JPA (recommended;
   update the routine's stack assumption) vs. migrate. No jOOQ codegen exists.
4. `ConversationController` builds send flows inline (recipient parsing, signature, sender
   selection, `sendReply` vs `sendNewEmail`). Extract into a service before adding the API
   so web and API share one path.
5. `blob_ref` is always NULL — attachments cannot be downloaded (roadmap item in CLAUDE.md).

## Missing tests
6. Audit test coverage of `ConversationController.send`/`createMessage` before refactor (#4).

## Tooling
7. `./mvnw`/`mvn` not yet run in this environment's first sync run (see CHANGELOG).
