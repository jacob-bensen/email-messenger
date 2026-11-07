# INTERNAL_TODO (backend)

Ordered by severity.

## Contract breakage
- No REST API / token auth exists; mobile client has nothing to call. Define `/api/v1` in `docs/API.md`, add stateless auth + controllers over the existing services. (CSRF-protected session-only today.)

## Logic drift
- Audit result 2026-11-07: no REST duplicates exist, so no divergence yet. Keep new API controllers thin over `ConversationListService`/`ChatService`/`ReplyService`; do not copy logic from `ConversationController` (it builds some model state inline, e.g. account colors, filter parsing — extract to a service before reuse).

## Missing tests / gates
- `mvn -o compile` could not run (parent POM not in offline cache); gates unverified on 2026-11-07 (docs-only run).

## Process / style
- Task template assumes jOOQ and `master`; code uses JPA, and sessions here are assigned feature branches. `CLAUDE.md` says never push to main.
- Existing history is in `master/CHANGELOG.md` (a *directory* named `master`, ~500KB) plus `master/{PLAN,BACKLOG,MASTER_ACTIONS}.md`; root `CHANGELOG.md` starts 2026-11-07.
- `CLAUDE.md` roadmap items remain (support email, Web Push, delivery states, signature cards, attachment storage, reply-to-bubble).
