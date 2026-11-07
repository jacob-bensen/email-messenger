# INTERNAL_TODO (backend)

## Contract breakage / blockers (highest first)
1. No REST API exists; mobile client has nothing to call. Needs `/api/v1/**`
   + stateless token auth (SecurityConfig currently session+CSRF only). Suggested first
   feature: token auth + `GET /api/v1/chats`.
2. Controllers hold sizeable orchestration (e.g. `ConversationController.send`
   picks sender account, signature, thread, attachments). Extract into a
   service before exposing via API, to avoid logic drift.

## Logic drift
- Routine assumes jOOQ; repo uses JPA. Treat JPA as authoritative; no jOOQ codegen.
- Routine says commit to `master`; this session's harness pins branch
  `claude/gifted-carson-kwum5d` and CLAUDE.md forbids pushing main. Branch rule followed.

## Missing tests / verification
- 2026-11-07: `./mvnw -q -B test` exited 0 (docs-only change; baseline green).

## Deferred
- Roadmap items in `CLAUDE.md` (support email, OAuth publish, Web Push, ...).
