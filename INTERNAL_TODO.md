# Internal TODO

Ordered by severity (contract breakage > logic drift > missing tests > style).

1. **Contract gap:** no `/api/v1` REST API or token auth; the mobile client has nothing to call.
   Start with login + `GET /api/v1/chats` + `GET /api/v1/chats/{key}`, reusing `ConversationListService`/`ChatService`.
2. **Logic drift risk:** conversation-building logic lives behind Thymeleaf-model controllers
   (`ConversationController.populateList`, `chatFor`); extract into a service returning records
   before exposing via REST so web and API cannot diverge.
3. Attachments are not downloadable (`blob_ref` null) — needed for mobile too (see CLAUDE.md roadmap).
4. `/chats/refresh` returns bare JSON with no error schema; define one in `docs/API.md`.
5. Routine prompt assumes jOOQ and `master`-only; this repo uses JPA and session branches. Reconcile.
6. Quality gates: `./mvnw verify` not yet run by the sync routine (see CHANGELOG for last result).
