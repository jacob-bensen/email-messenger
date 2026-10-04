# INTERNAL_TODO (ordered by severity)

## Contract breakage
- [ ] No `/api/v1` JSON API exists; mobile client has nothing to call. Pick auth scheme
      (bearer tokens vs. session) and implement the first slice: login + chats list +
      chat detail (see "Proposed v1" in `docs/API.md`). Needs a Flyway V36 if tokens are stored.
- [ ] Session/CSRF-only security (`SecurityConfig`): add a stateless chain for `/api/**`.

## Logic drift
- [ ] `ConversationController` (504 lines) parses mailbox/filter params and orchestrates
      send flow inline; extract to a service before an API controller needs the same logic.
- [ ] Stack assumption mismatch: routine assumes jOOQ; backend uses JPA. Treat JPA as canonical.

## Process
- [ ] Routine says commit to `master`; this session was bound to feature branches
      (`claude/gifted-carson-stwc21` here, `claude/trusting-cori-stwc21` in mobile). Work was
      pushed to those branches. Decide which policy wins.
- [ ] Two roadmaps/doc systems exist: `CLAUDE.md` roadmap and `master/{PLAN,BACKLOG,MASTER_ACTIONS}.md`.
      Reconcile.

## Missing tests
- [ ] None audited yet for controllers' web/API parity (no API yet).
