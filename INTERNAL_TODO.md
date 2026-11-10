# INTERNAL_TODO (backend)

## Contract breakage
- No mobile-facing JSON API or bearer-token auth exists; mobile repo has no code. First feature run should add
  `/api/v1/auth/login` + `GET /api/v1/chats` (read-only) over existing services. (Needs decision: token model.)
- SSE event name/payload at `/chats/events` is undocumented.

## Logic drift
- Mailbox selection / scope resolution (`selectMailbox`, `scopeAddress`) lives in `ConversationController`;
  extract to a service before an API controller needs it.
- Stack mismatch vs. routine assumptions: backend uses JPA, not jOOQ — no codegen step.

## Missing tests
- Not verified this run: `./mvnw test` (see CHANGELOG 2026-11-10).

## Deferred
- CLAUDE.md roadmap items (support email, Google OAuth publish, Web Push, delivery states, …).
- Routine says work on `master`; this session's harness pinned branch `claude/gifted-carson-g38ug5`, and CLAUDE.md
  says not to push to main — work was committed to the session branch.
