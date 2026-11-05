# Internal TODO (ordered by severity)

## Contract breakage
1. No token-authenticated JSON API exists; mobile client cannot talk to the server. Session+CSRF only. Design `/api/v1` + Bearer auth (docs/API.md §Planned).
2. `/chats/events` SSE payload schema is undocumented.

## Logic drift
3. Stack differs from the routine's assumption: persistence is JPA (not jOOQ); no codegen step. Docs reflect reality.
4. Frontend repo `conexus-mail-mobile` has no Flutter project yet (README only) — `flutter create` + `lib/domain` mirroring needed.

## Missing tests / tooling
5. Only `java` and `mvn` are available in the routine env; `flutter`/`dart` are not installed, so frontend gates can't run.
6. Backend gates not run this run (docs-only change).

## Process
7. Routine prompt says commit to `master`; session harness mandates branches `claude/gifted-carson-ltanqd` (backend) and `claude/trusting-cori-ltanqd` (mobile) and CLAUDE.md forbids pushing main. Branches used; reconcile.
8. Backend CLAUDE.md roadmap has open items (support email, Google OAuth publish, Web Push, delivery states, ...).
