# INTERNAL_TODO (backend)

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
- No `/api/v1` exists; the mobile client has nothing to call. Needs auth decision (token vs.
  session+CSRF) — see `docs/API.md`.

## Logic drift risk
- `ConversationController.send` contains orchestration (recipient resolution, sender-account
  choice, signature append, reply-vs-new branching, outbound recording). Extract into a service
  (e.g. `ChatSendService`) before exposing a REST send endpoint so web and API share it.
- Same for list/open-chat mailbox scoping helpers (`selectMailbox`, `scopeAddress`) in the controller.

## Stack deviations from task template
- Persistence is JPA, not jOOQ; no codegen step. Keep as is.

## Missing tests / unverified
- `./mvnw test` could not run in the Phase 0 sandbox (Maven deps not cached offline); rerun with network.
- IMAP IDLE unverified against a real server (see CLAUDE.md roadmap).

## Process notes
- Routine prompt says commit to `master`; this session's harness mandated branch
  `claude/gifted-carson-f1efo1`, and CLAUDE.md forbids pushing to main. Branch rule followed.
