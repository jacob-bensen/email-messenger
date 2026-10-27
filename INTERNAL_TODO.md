# Internal TODO (backend)

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
- [ ] No mobile REST API or token auth exists (`docs/API.md` §2 is a proposal). Mobile
      client cannot log in: security chain is session+CSRF with HTML redirects.
      First step: `/api/**` SecurityFilterChain + `POST /api/v1/auth/token` + JSON error envelope.
- [ ] `GET /chats/events` event payload schema undocumented.

## Logic drift (web vs API sharing one service layer)
- [ ] `ConversationController.send` / `createMessage` hold business logic (mailbox
      scoping, signature append, reply-vs-new decision, attachment conversion). Extract
      to a service (e.g. `ChatSendService`) before the API reuses it.
- [ ] `ConversationController.selectMailbox/scopeAddress/sendingAccount` — same.
- [ ] `ReplyService` always threads onto the last message (see CLAUDE.md roadmap).

## Missing tests
- [ ] No tests for any JSON/API surface (none exists yet).

## Style / docs
- [ ] Stack differs from routine template: JPA instead of jOOQ. Keep; do not introduce jOOQ.
- [ ] Pre-existing planning docs live in `master/` (`PLAN.md`, `BACKLOG.md`,
      `MASTER_ACTIONS.md`, 500KB `CHANGELOG.md`). Root `CHANGELOG.md` is the routine's log;
      history is in `master/CHANGELOG.md`. Consider consolidating.
- [ ] Branch policy: routine says commit to `master`; this session's harness restricts
      pushes to designated `claude/*` branches. Resolve with the owner.
