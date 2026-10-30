# Internal TODO (ordered by severity)

## Contract breakage
- No `/api/v1` exists; mobile client has nothing to call. Define in `docs/API.md` first. Needs bearer-token auth (session+CSRF is unsuitable).

## Logic drift
- `ConversationController` holds mailbox-scope selection (`selectMailbox`, `scopeAddress`) — extract to a service before an API reuses it.

## Process / routine
- Routine prompt says "master only, no PRs, jOOQ"; this repo uses JPA and the session harness designates branch `claude/gifted-carson-n8s191` (sibling: `claude/trusting-cori-n8s191`). Work was committed on the designated branches, not master. Decide and reconcile.
- No Flutter/Dart toolchain in the routine environment; frontend gates can't run.
- Maven deps not cached in sandbox; `mvn verify` not run in the doc-only run of 2026-10-30.

## Missing tests / style
- (none recorded)
