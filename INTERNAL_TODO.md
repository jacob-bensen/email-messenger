# Internal TODO (sync audit 2026-11-06)

## Contract breakage
- No `/api/v1` exists and there is no non-cookie auth; a mobile client cannot sign in.
  Design auth first (see `docs/API.md`), then conversations list/detail.
- Existing JSON/SSE endpoints (`/chats/refresh`, `/chats/events`) require session + CSRF.

## Logic drift
- Controllers (`ConversationController`, `MailboxController`) hold param parsing,
  mailbox scoping (`selectMailbox`, `scopeAddress`) and filter parsing; extract into
  services before a REST layer so both share it.

## Tech debt / deviations
- Persistence is JPA, not jOOQ as the generic routine assumes. Do not migrate; update routine.
- Root dir `master/` holds BACKLOG.md, CHANGELOG.md, MASTER_ACTIONS.md, PLAN.md (not reviewed this run); decide whether it overlaps with docs/ and CHANGELOG.md.
- Dockerfile/docker-compose.yml/.github exist although CLAUDE.md says Docker/CI were removed.
- Routine says commit to `master`; sessions are pinned to `claude/*` branches. Reconcile.

## Missing tests
- No controller-level tests exist for a REST layer (none to test yet).

## Not run this session
- `mvn verify` not executed (docs-only change).
