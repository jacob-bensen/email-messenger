# Internal TODO

## Contract breakage
- No `/api/v1` REST API exists; mobile client has nothing to call. Needs token auth (session+CSRF is browser-only), conversation list/messages/send endpoints built on `ConversationService`/`ReplyService`/`OutboundMessageService`.
- SSE `/chats/events` payload undocumented in API.md.

## Logic drift
- Routine prompt assumes jOOQ; backend uses Spring Data JPA. Treat JPA as authoritative.
- `CLAUDE.md` says Docker was removed, but `Dockerfile` and `docker-compose.yml` are still in the repo root.
- Roadmap says tests are local only; `./mvnw test` not run during this docs-only pass.

## Missing tests
- No contract tests for `/chats/refresh` JSON shape (verify).

## Style / process
- Scheduled routine says commit to `master`; session is pinned to branch `claude/gifted-carson-sitmig` (and `claude/trusting-cori-sitmig` for mobile), which was followed.
