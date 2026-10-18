# INTERNAL_TODO (ordered by severity)

## Contract breakage
- No `/api/v1` exists; `conexus-mail-mobile` has no code. Everything the mobile app needs is TBD. (Phase 0 audit, 2026-10-18)
- No mobile-capable auth (session + CSRF only). Decide bearer-token scheme before any API work.

## Logic drift
- None detected (no API to drift from). Keep controllers thin: `ConversationController` should delegate to `ChatService`/`ReplyService`.

## Process / environment
- Routine assumes jOOQ, `master` branch and Flutter; repo uses JPA, designated branches, and neither `flutter` nor `dart` is installed in this environment.
- `master/` directory holds legacy PLAN/BACKLOG/CHANGELOG (504 KB CHANGELOG.md) — new run entries go in root `CHANGELOG.md`.

## Missing tests
- Not yet audited this run (`./mvnw test` not executed during discovery).
