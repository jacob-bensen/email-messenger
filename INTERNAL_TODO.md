# INTERNAL_TODO (backend)

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
- [ ] No mobile REST API exists; `docs/API.md` has only a "Planned" stub. Client repo cannot function. Next run: design token auth + read-only conversation list/detail in API.md first.
- [ ] No token/bearer auth for non-browser clients (session + CSRF only).

## Logic drift
- [ ] `ConversationController` (~370 lines) contains send/reply orchestration (attachments, thread vs new-message branch, flash messages). Extract into a service so a future API shares it.
- [ ] `ReplyService` always threads onto the last message (already on the CLAUDE.md roadmap).

## Docs / process
- [ ] Generic routine assumes jOOQ; this repo uses JPA. Routine text should be adjusted or accepted as is.
- [ ] `master/` directory (PLAN/BACKLOG/CHANGELOG from an older "MailIM" product framing, Personal/Team/Enterprise pricing) is stale vs code (FREE/PRO/BUSINESS). Reconcile or delete.
- [ ] Routine says commit to `master`; the session is pinned to a feature branch (`claude/gifted-carson-e2fct8`) and CLAUDE.md forbids pushing to main. Branch policy needs a human decision.

## Missing tests
- [ ] Not audited this run (tests not executed; see CHANGELOG).
