# INTERNAL_TODO (backend) — ordered by severity

## Contract breakage
1. **No mobile API or token auth exists.** Only session+CSRF HTML. Decide token scheme (JWT vs opaque
   DB-backed tokens), add `/api/**` stateless filter chain in `SecurityConfig`, implement `docs/API.md`.
2. Mobile repo (`conexus-mail-mobile`) contains only a README — no Flutter project yet.

## Logic drift
3. Sending logic lives partly in `ConversationController.send` (signature append, sender choice,
   recipient derivation, asReply branching). Extract into a service before an API reuses it.
4. Same for `selectMailbox`/`scopeAddress` mailbox scoping helpers in the controller.

## Brief vs reality
5. Brief assumes jOOQ; project uses JPA. Brief says commit to `master`; session rules pin work to
   feature branches (backend `claude/gifted-carson-psyjw1`) — this run followed the branch rules.

## Missing tests / docs
6. No tests assert API contract (none exists). Add MockMvc contract tests with the API.
7. Roadmap items in CLAUDE.md remain (Web Push, delivery states, reply-to-bubble, shared files…).
