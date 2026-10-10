# INTERNAL_TODO (ordered by severity)

## Contract breakage
- No mobile REST API exists; `docs/API.md` planned surface is TBD. Mobile repo has no code, so no mismatches yet.
- No non-interactive auth (token/API key). Session+CSRF only. Decide: Spring Security bearer/JWT for `/api/**`.

## Logic drift risk
- When `/api/v1` is added, controllers must call the same services as `ConversationController` (ChatService, ReplyService).
  Today `ConversationController` is large; extract logic into services first if any is controller-embedded (TBD audit).

## Missing tests
- None for an API layer (doesn't exist).

## Style / process
- Stack differs from routine's assumption: JPA (no jOOQ), no codegen. Scheduler prompt says `master`; this session's
  harness pins branch `claude/gifted-carson-9zem2v` — pushes go there.
- Existing CLAUDE.md roadmap items remain open (support email, Google OAuth publish, Web Push, delivery states, ...).
