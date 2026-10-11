# INTERNAL_TODO (ordered by severity)

## Contract breakage
- No REST API exists; mobile client has no endpoints. Needs token auth + `/api/v1`
  (conversations list, conversation messages, send reply, mailbox list) — first
  feature to build; define in docs/API.md first.

## Logic drift
- Web logic lives in controllers like `ConversationController` (large). Before adding
  `/api/v1`, check which logic must move to services so both share it.

## Missing tests / verification
- `./mvnw` could not resolve the project in this sandbox (offline Maven; `mvn` at
  /opt/maven only). Build/test not run on 2026-10-11 — verify in a networked env.
- Flutter/Dart not installed in this environment.

## Process
- Routine prompt assumes jOOQ + `master`-only; repo uses JPA and the harness pins
  work branches (`claude/gifted-carson-23gddl`). Reconcile prompt with repo.
- Roadmap items in CLAUDE.md (support email, Google OAuth publish, Web Push,
  delivery states, ...) remain; see CLAUDE.md.
