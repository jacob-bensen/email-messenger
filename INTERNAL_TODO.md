# Internal TODO (backend)
Ordered by severity.

## Contract breakage / blockers
- No REST API or token auth exists; the mobile client has nothing to call. Design in docs/API.md §Planned.
  Needs `/api/**` security chain (stateless Bearer, CSRF off for it) — separate `SecurityFilterChain`.

## Logic drift
- `ConversationController.send` (~/chats/{key}/send) holds send-orchestration logic (recipient resolution, signature,
  asReply vs new thread, attachments). Extract into a service before an API endpoint reuses it, to avoid web/API forks.
- `ConversationController.chat/chatPane` call `chatService.markRead` inline; keep in service for API reuse.

## Missing tests
- No tests exist for `/chats/refresh` JSON shape or SSE payload contract (verify).

## Docs / style
- SSE event payload undocumented in docs/API.md.
- Generic routine brief assumes jOOQ; project uses JPA — no codegen.
- Routine brief says commit to `master`; this session was bound to feature branch `claude/gifted-carson-uc1y2p`
  (and `claude/trusting-cori-uc1y2p` for mobile). Reconcile the schedule config.
