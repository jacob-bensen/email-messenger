# INTERNAL_TODO

Ordered by severity: contract breakage > logic drift > missing tests > style.
(The existing `master/` directory holds an older routine's PLAN/BACKLOG/CHANGELOG; the
product roadmap lives in `CLAUDE.md`.)

## Contract breakage
- [ ] **No REST API / token auth exists.** Mobile client cannot authenticate or read data.
      Implement `/api/v1` per `docs/API.md` §2 (bearer auth chain in `SecurityConfig`, separate
      stateless `SecurityFilterChain` for `/api/**` with CSRF off).
- [ ] `/chats/events` SSE payload is undocumented in API.md (TBD).

## Logic drift / structure
- [ ] Send flow orchestration (`sendReply`/`sendNewEmail` + `recordReply`/`recordNewEmail`, recipient
      parsing, sending-account selection) lives in `ConversationController`; extract to a service
      so web and API share it.
- [ ] Task spec assumes jOOQ; backend uses JPA/Hibernate. Spec assumption is wrong — no codegen step.

## Missing docs/tests
- [ ] Attachments are not downloadable (`blob_ref` null) — roadmap item in CLAUDE.md.
- [ ] No `/api` tests (nothing to test yet).

## Environment
- Flutter/Dart not installed in the routine sandbox; mobile gates (`dart format`, `flutter analyze`,
  `flutter test`) could not run on 2026-10-08.
- Routine prompt says commit to `master`; the session was pinned to feature branches
  (`claude/gifted-carson-06s9w7`, `claude/trusting-cori-06s9w7`) and the backend CLAUDE.md says
  never push to main, so work was pushed to those branches. Owner to decide if this should change.
