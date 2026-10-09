# Internal TODO (sync audit, ordered by severity)

## Contract breakage
- No mobile-facing REST API and no token auth: every route is session+CSRF. The mobile
  repo (`conexus-mail-mobile`) contains only a README, so there is no client to break yet.
  Next feature run: define `/api/v1` auth + conversation list/detail in `docs/API.md`, then implement.

## Logic drift
- `ConversationController` holds mailbox-scope helpers (`selectMailbox`, `scopeAddress`,
  `chatFor`); extract into a service so an API controller can share them.
- Roadmap in `CLAUDE.md` (support email, Google OAuth publish, Web Push, delivery states, …)
  is separate from the mobile roadmap in the other repo.

## Missing tests / tooling
- Phase-0 quality gate result recorded in `CHANGELOG.md` for 2026-10-09.

## Style / template mismatches
- Routine template assumes jOOQ; repo uses JPA. No codegen. Treat template as "persistence layer".
- Routine says commit to `master`; this session is restricted to feature branch
  `claude/gifted-carson-1xlilz` (do not push `master`). CLAUDE.md also says never push `main`.
