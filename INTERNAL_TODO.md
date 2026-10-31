# Internal TODO (backend)

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
- [ ] **No REST API / token auth for the mobile client.** `docs/API.md` §2 is design only.
      Mobile repo (`conexus-mail-mobile`) contains only a README — nothing to break yet.
      Next feature run: token auth + `GET /api/v1/chats` + `GET /api/v1/chats/{key}`.
- [ ] Run prompt assumes jOOQ; the project uses JPA/Hibernate. Decision: keep JPA, no jOOQ
      (matches repo conventions). Revisit only if the owner asks.

## Logic drift (web vs. future API)
- [ ] Send orchestration (recipient parsing, sending-account choice, signature append,
      `replyService` + `outboundMessageService.recordNewEmail`) lives in
      `ConversationController`. Extract to a service before exposing via API.
- [ ] Mailbox scope resolution (`selectMailbox`, `scopeAddress`, `accountColorMap`) is
      controller-private; same extraction needed.

## Missing tests / verification
- [ ] IMAP IDLE unverified against a real server (see CLAUDE.md roadmap).
- [ ] No tests for `/chats/refresh` JSON shape or `/chats/events` event format.

## Process
- [ ] Routine prompt says commit to `master`; this session is pinned to feature branches
      (`claude/gifted-carson-5c1oup`, `claude/trusting-cori-5c1oup`) and the backend
      CLAUDE.md forbids pushing to main. Followed the branch instructions.
- [ ] Flutter/Dart not installed in the cloud environment → frontend gates can't run here.
