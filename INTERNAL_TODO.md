# Internal TODO (severity-ordered)

## Contract breakage
1. **No mobile API exists.** Client repo is an empty Flutter placeholder; server has no `/api/**`, no token auth. Implement `docs/API.md` §2 (auth first).
## Logic drift
2. `ConversationController.send` holds send orchestration (reply vs new, signature, recording outbound). Extract to a service (e.g. `ChatSendService`) before exposing via API.
3. `ConversationController` chat-loading helpers (`chatFor`, `selectMailbox`, `scopeAddress`) are controller-private; move to `ChatService` for API reuse.
## Missing tests / tooling
4. Maven deps not cached in this environment; `mvn verify` not yet run in a routine run.
5. Roadmap in `CLAUDE.md` (support email, Google OAuth publish, Web Push, delivery states…) still open.
## Style
6. Routine prompt assumes jOOQ; backend uses JPA. Do not introduce jOOQ without a decision.
7. Routine prompt says commit to `master`; session harness mandates branch `claude/gifted-carson-dsiem6` — pushed there instead.
