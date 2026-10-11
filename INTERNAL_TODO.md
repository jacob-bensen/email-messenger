# INTERNAL_TODO (backend)

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
1. **No REST API / token auth for the mobile client.** Only session+CSRF HTML exists. Build `/api/v1` (stateless bearer filter chain, `@RestController`s over `ChatService`, `ConversationListService`, `ReplyService`/`OutboundMessageService`), then update docs/API.md first. Highest-value next feature (needs both repos).
2. Attachments not downloadable (`blob_ref` null) — blocks mobile attachment viewing (see CLAUDE.md roadmap).

## Logic drift
3. `ChatService`, `ConversationListService`, `ChatService.markRead` are package-private in `web`; extract/widen for API reuse without duplicating controller logic.
4. Routine assumes jOOQ; backend uses JPA. Treat JPA as authoritative.

## Missing tests
5. Not audited this run (tests were not executed; see CHANGELOG).

## Process conflicts
6. Scheduled routine says "commit to master"; session/CLAUDE.md say never push to master. Work was done on the designated session branches instead.
