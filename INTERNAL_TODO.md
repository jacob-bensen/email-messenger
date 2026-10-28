# Internal TODO (sync audit, 2026-10-28)

Ordered by severity.

## Contract breakage
1. **No REST API for the mobile client exists.** Server only exposes session/CSRF-protected HTML; unauthenticated → 302. Need token auth + `/api/v1` (see docs/API.md PROPOSED). Mobile repo is an empty placeholder, so nothing is currently *broken*, only absent.
2. No bearer/token auth chain in `SecurityConfig`.

## Logic drift
3. Business rules live partly in controllers (`ConversationController`: reply-empty validation, mailbox scope selection `selectMailbox`/`scopeAddress`). Extract into services before exposing via API to avoid divergent logic.
4. Tech-stack mismatch vs. routine assumptions: persistence is JPA (no jOOQ codegen).

## Missing tests
5. Not audited this run (no source changes made).

## Style / tooling
6. `docs/` did not exist before this run; WEB.md route table derived from `@*Mapping` scan — verify service column on next touch.
7. Quality gates this run: see CHANGELOG.
8. Branching: routine says `master`, but the session was bound to `claude/gifted-carson-55rne0` (and `claude/trusting-cori-55rne0` for mobile); repo CLAUDE.md also forbids pushing to main. Work was pushed to the session branches.
