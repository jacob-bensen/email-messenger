# INTERNAL_TODO (backend)

Ordered by severity.

## Contract breakage
- No REST API / token auth exists; mobile client has nothing to call. Define
  `/api/v1` in `docs/API.md` first (auth, conversation list/detail, send),
  then implement on top of existing services.
- CSRF + session-cookie auth is unsuitable for mobile; add a stateless
  `/api/**` filter chain (bearer tokens), CSRF-exempt.

## Logic drift
- Audit not yet done in depth: `ConversationController` should be checked for
  business logic that REST endpoints would need (mailbox/scope selection
  `selectMailbox`/`scopeAddress`, send flow) — extract into a service before
  exposing via API.
- Target stack says jOOQ; backend uses JPA/Hibernate. Decision: keep JPA
  (existing code style wins); do not introduce jOOQ.

## Docs / process drift
- Routine says commit to `master`; CLAUDE.md says never push to main and the
  session harness assigns `claude/*` branches. This run followed the assigned
  branches. Owner should reconcile.
- CLAUDE.md says Dockerfile/docker-compose were removed 2026-06-11, but both
  files exist in the repo root. Remove or update CLAUDE.md.
- `docs/API.md` SSE event payload schema TBD.

## Missing tests
- None identified yet for API (none exists).
