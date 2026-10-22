# Internal TODO (backend)

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
- [ ] No client REST API and no token auth exist; the mobile repo has no code. Decide auth
      scheme, then add `/api/v1` (see `docs/API.md` §2). Blocks all mobile work.
- [ ] `docs/API.md` §1: SSE event payload and webhook error statuses are TBD — read the code and fill in.

## Logic drift (risk once an API exists)
- [ ] `ConversationController` contains orchestration (mailbox scoping, signature append,
      reply-vs-new-thread in `send`, recipient parsing). Extract into a service the web and API both call.
- [ ] Spec assumed jOOQ; repo uses JPA. Treat JPA as authoritative (documented in `docs/BACKEND.md`).

## Missing tests
- [ ] Add contract tests for `/chats/refresh` JSON shape and SSE event payload once documented.

## Housekeeping
- [ ] `master/` (tracked: BACKLOG.md, CHANGELOG.md, MASTER_ACTIONS.md) is the older routine's changelog/backlog; this routine's `CHANGELOG.md` is separate at repo root. Decide whether to merge them.
- [ ] Routine spec says commit to `master`; this run used the session's designated branches instead.
