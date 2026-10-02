# INTERNAL_TODO

Known gaps, tech debt, deferred work. Ordered by severity
(contract breakage > logic drift > missing tests > style). Updated 2026-10-02.

## Contract breakage
- [ ] **No mobile REST API exists.** `docs/API.md` §3 defines a planned `/api/v1`;
      nothing is implemented, and the mobile repo cannot authenticate (session +
      CSRF only). First feature to build: token auth + `GET /api/v1/mailboxes` +
      `GET /api/v1/conversations` (read-only vertical slice), with matching Dart
      DTOs.
- [ ] `/chats/events` SSE payload shape is undocumented — read `ChatEventStream`
      and pin it in `API.md`.

## Logic drift
- [ ] Controllers currently own some orchestration (`ConversationController`
      mailbox selection / scope resolution: `selectMailbox`, `scopeAddress`).
      Extract into a service before the API layer reuses it.

## Missing tests
- [ ] `./mvnw` cannot download Maven in the cloud sandbox (wget to repo.maven.apache.org fails); use system `mvn`. Full test suite was NOT run on 2026-10-02 (docs-only change); `mvn -q -B test-compile` passed.

## Docs / process
- [ ] Repo already has `master/` (BACKLOG.md, CHANGELOG.md, MASTER_ACTIONS.md) from
      an earlier routine. Root `CHANGELOG.md` is new; decide whether to merge or
      retire `master/CHANGELOG.md`.
- [ ] Task spec says jOOQ; the code uses JPA. Spec assumption, not a bug.
- [ ] Spec says commit to `master`; this session is pinned to feature branches
      (`claude/gifted-carson-9yes3o` / `claude/trusting-cori-9yes3o`) by the
      environment, and CLAUDE.md forbids pushing to `main`. Work was pushed to the
      session branches instead.
