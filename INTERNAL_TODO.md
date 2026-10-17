# INTERNAL_TODO (ordered by severity)

## Contract breakage
1. **No mobile API or token auth.** Backend exposes only session+CSRF HTML. Client repo is empty. Needs an `/api/v1` design (docs/API.md), bearer/JWT auth chain in `SecurityConfig` (separate `securityMatcher("/api/**")`, CSRF off), and controllers.
## Logic drift
2. Send/compose logic lives in `ConversationController` (signature, sender-account choice, reply-vs-new). Extract to a service (e.g. `ChatSendService`) before adding an API.
3. `selectMailbox`/`scopeAddress` scoping rules are private to `ConversationController`; same extraction.
4. Prompt assumed jOOQ; project uses JPA — no codegen. CLAUDE.md is authoritative.
## Missing tests / docs
5. docs/API.md `/chats/events` payload schema TBD.
6. No tests exist yet for a REST layer (none exists).
## Process notes
7. Routine prompt says "master only"; this session's harness mandates dedicated branches (`claude/gifted-carson-dh5br5`, `claude/trusting-cori-dh5br5`), so commits went there.
