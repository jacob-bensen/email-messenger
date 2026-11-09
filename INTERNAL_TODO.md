# INTERNAL_TODO (backend)

## Contract breakage
- No REST API / token auth for the mobile client; session+CSRF only. Blocks every mobile feature.
- `/chats/events` `new-mail` payload shape undocumented.

## Logic drift
- None found (single web surface). When REST is added, route through `ChatService`/`ConversationListService`/`OutboundMessageService`; do not copy controller logic (several controllers resolve mailbox scope in-controller: `ConversationController.selectMailbox/scopeAddress` — extract to a service first).

## Spec deviations
- Routine assumes jOOQ; repo uses JPA. Kept JPA (match existing code).
- Routine says commit to `master`; this session's harness mandates branch `claude/gifted-carson-6zrjwr`, and CLAUDE.md forbids pushing to main. Used the branch.

## Missing tests / debt
- Test suite not run this session (see CHANGELOG).
- Roadmap items in CLAUDE.md remain (support email, Google OAuth publish, Web Push, delivery states, ...).
