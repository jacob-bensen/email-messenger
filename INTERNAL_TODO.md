# INTERNAL_TODO (cross-repo sync) — ordered by severity

Product roadmap lives in `CLAUDE.md`; operator/human actions in `master/MASTER_ACTIONS.md`.

## Contract breakage
1. **No mobile API or token auth.** Server only has session+CSRF browser auth and HTML pages; mobile repo
   is an empty scaffold. Next feature run: define `/api/v1` auth (token issue/refresh) + conversation
   list/detail/send in `docs/API.md`, backed by the existing services (`ConversationListService`,
   `ChatService`, `OutboundMessageService`).
2. `/chats/events` SSE payload and `/chats/refresh` error shape undocumented → pin down in API.md.
3. No JSON error envelope (HTML `error.html` only).

## Logic drift
4. Controller-level view assembly in `ConversationController` (model building in `populateList`) —
   a REST layer must reuse services, not copy this.

## Missing tests
5. Not run this session: `./mvnw test` (see CHANGELOG.md for what was run).

## Style / process
6. Routine template assumes jOOQ + master-branch workflow; repo uses JPA and the session-designated
   branch (`claude/gifted-carson-onzwgl`). Docs/CLAUDE.md reflect the real stack.
