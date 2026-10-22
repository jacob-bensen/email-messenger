# INTERNAL_TODO

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
- No mobile REST API exists; `docs/API.md` has nothing for the Flutter client
  to call. Mobile repo (`conexus-mail-mobile`) contains only a README. First
  feature to specify: token auth + read-only conversation list/messages.
- Auth is session-cookie + CSRF only; a mobile client needs a token scheme
  (decision TBD: JWT vs opaque bearer in `SecurityConfig`, separate filter chain
  under `/api/**`, CSRF-exempt).
- No JSON error envelope; errors are HTML (`GlobalExceptionHandler`).

## Logic drift
- Web controllers (e.g. `ConversationController`) assemble view models partly
  inline; before adding an API, extract shared logic into `ChatService` /
  `ConversationListService` so web and API use one path. Audit pending.
- Routine assumes jOOQ; repo uses JPA. Update the routine prompt or accept JPA.

## Missing tests
- No contract tests for `POST /chats/refresh` JSON shape (verify).
- SSE payload (`/chats/events`) undocumented.

## Process notes
- Routine says "commit to master"; this session was restricted to branch
  `claude/gifted-carson-0vp4qo` (backend) / `claude/trusting-cori-0vp4qo`
  (mobile) by the environment, so work was pushed there.
- Existing history log is `master/CHANGELOG.md` (~500 KB); root
  `CHANGELOG.md` is new and brief.
- `./mvnw test` not run in this pass (docs-only change).
