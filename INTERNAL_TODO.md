# INTERNAL_TODO

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
- [ ] No mobile JSON API exists; `docs/API.md` §Planned is TBD. Needs bearer-token auth (session+CSRF is unusable
      from mobile), `/api/v1/*` controllers, and a Flyway migration for refresh tokens. First feature to build.
- [ ] `GET /chats/events` SSE payload and `/billing/webhook` error codes are undocumented (TBD in API.md).

## Logic drift (web vs. future API)
- [ ] `ConversationController` holds business rules that a REST layer would have to duplicate: `sendingAccount`
      (default-account fallback), `withSignature`, `parseRecipients`, `scopeAddress` (single-mailbox no-scope rule),
      and the send → `outboundMessageService.record*` orchestration. Extract into a service (e.g. `ChatSendService`)
      before adding API endpoints.
- [ ] `ReplyService` always threads onto the thread's last message (roadmap item in CLAUDE.md).

## Missing tests
- [ ] No controller/service tests exist for a mobile API (none implemented).

## Style / docs
- [ ] Prior run notes live in `master/` (PLAN.md, BACKLOG.md, CHANGELOG.md 500KB, MASTER_ACTIONS.md); the new
      root `CHANGELOG.md` starts fresh and points there.
- [ ] Task prompt says "master only"; this session is pinned to `claude/gifted-carson-vzpu0c` and CLAUDE.md forbids
      pushing to main — branch rule followed from the session, not the prompt.
