# INTERNAL_TODO (backend) — ordered by severity

## Contract breakage
- [ ] No REST API / token auth at all; mobile client has nothing to call. Implement DRAFT `/api/v1` from `docs/API.md` (start: bearer login + `GET /api/v1/chats`, `GET /api/v1/chats/{key}`).
- [ ] Security chain is session+CSRF only; add a stateless `/api/**` filter chain.

## Logic drift / structure
- [ ] Controller-resident orchestration (`ConversationController.populateList`, `selectMailbox`, `scopeAddress`, `accountColorMap`, `/chats/new`) should move into a service so API and web share it.
- [ ] `ChatEventStream` SSE payload undocumented; decide mobile live-update path (SSE+bearer vs Web Push — roadmap item "Live delivery part 2").
- [ ] Attachment `blob_ref` always null: needed before mobile can show/download attachments.

## Missing tests
- [ ] None yet for any API (none exists).

## Docs / process
- [ ] Task routine assumes jOOQ; backend uses JPA. Keep docs saying JPA.
- [ ] Routine says work on `master`; this environment mandates feature branches (`claude/gifted-carson-29d43f`). Merge to master is a human step.
- [ ] `master/` directory is the legacy planning log; consider consolidating into root docs.
