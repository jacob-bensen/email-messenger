# INTERNAL_TODO (backend)
Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
- No mobile REST API exists (`docs/API.md` §B is a proposal). Blocks all client features. Needs: token auth design + migration (V36), `/api/v1` read-only slice reusing `ChatService`/`ConversationListService`/`ReplyService`.
- `/chats/events` SSE payload and `/chats/refresh` JSON keys are undocumented (TBD in API.md); read `ChatEventStream` / `ConversationController.refresh` and document.

## Logic drift
- Controllers resolve users and call services directly (good); verify no business rules live in templates/controllers when `/api/v1` is added (e.g. plan-limit checks in `ConversationController.createMessage`).
- Stack deviation: routine assumes jOOQ; repo uses JPA. Do not introduce jOOQ without a decision.

## Missing / hygiene
- Repo contains a stray `master/` dir (BACKLOG, CHANGELOG, MASTER_ACTIONS, PLAN) tracked in git — predates this routine; root `CHANGELOG.md` now exists too. Consolidate when convenient.
- Task/branching conflict: routine says commit to `master`; this session's harness mandates `claude/gifted-carson-y4rar9`. Work was pushed to the designated branch.
- Roadmap items in `CLAUDE.md` (Web Push, delivery states, attachments storage) will need API surface for mobile.
