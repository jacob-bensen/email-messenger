# INTERNAL_TODO (backend)

Ordered by severity.

## Contract breakage
- No REST API / token auth exists; `conexus-mail-mobile` has nothing to call. Design `docs/API.md` v1 (auth first), then implement.
- `/chats/events` SSE payload and `/chats/refresh` response are undocumented-by-test; pin them with tests before the client depends on them.

## Logic drift
- `ConversationController` (504 lines) holds send/recipient/attachment orchestration inline (`send`, `createMessage`). Extract into a service so a future `/api/v1` controller shares it.
- Generic routine template assumes jOOQ; project uses JPA. Keep JPA.

## Missing tests
- Not yet verified: IMAP IDLE against a real server (see CLAUDE.md roadmap).

## Style
- README still says "MVP is being scaffolded" and lists attachment download links (not implemented; `blob_ref` null).
