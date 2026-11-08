# Internal TODO (backend)

Ordered by severity (contract breakage > logic drift > missing tests > style).

## Contract breakage
1. No `/api/v1` REST API or token auth exists; the mobile client cannot talk to the
   server at all. Next run: define slice 1 in `docs/API.md` (login token, list chats,
   chat detail, send reply), then implement over `ConversationListService`,
   `ChatService`, `ReplyService`.
2. SSE `NewMailEvent` payload undocumented (TBD in API.md).

## Logic drift
3. Business logic lives partly in `ConversationController` (recipient parsing,
   mailbox selection, signature appending, key computation). Extract into a service
   before adding an API controller so both share it.

## Missing tests
4. Maven build not run in this environment (offline; parent POM unresolvable). Docs-only change, so no code was affected.

## Style / docs
5. `README`/`DEPLOY.md` still mention Docker/CI that CLAUDE.md says were removed.
