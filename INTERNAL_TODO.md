# INTERNAL_TODO (ordered by severity)

## Contract breakage
- No REST API / token auth exists; mobile client has nothing to call. Next run:
  pick a first slice (suggest `POST /api/v1/auth/login` + `GET /api/v1/chats`),
  define schemas in docs/API.md first, then implement via existing services.

## Logic drift
- None found: only the web layer exists, controllers use the service layer.
  Re-audit once an API layer is added.

## Missing tests / tech debt
- Roadmap items in CLAUDE.md remain (support email, Google OAuth publish, Web
  Push, delivery states, signature contact card, files/links, reply-to-bubble).
- Routine assumes jOOQ; project uses JPA — do not introduce jOOQ without a decision.
- Routine says commit to `master`; the session harness mandates the
  `claude/*` branches. Work was pushed to the harness branches.
- DEPLOY.md/Dockerfile/docker-compose.yml still exist though CLAUDE.md says Docker was removed.
- `master/` dir holds the routine's older BACKLOG/CHANGELOG/PLAN notes; reconcile with root CHANGELOG.md and INTERNAL_TODO.md.
