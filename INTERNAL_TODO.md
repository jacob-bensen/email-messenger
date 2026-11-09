# Internal TODO (ordered by severity)

## Contract breakage
- [ ] No REST/JSON API exists; the mobile client cannot talk to the backend. Design `/api/v1` (token auth + conversations list/detail/read/send/mailboxes) per `docs/API.md`, reusing existing services. **Recommended next feature run.**
- [ ] No token (bearer) auth; only session cookies + CSRF.

## Logic drift
- [ ] Routine assumes jOOQ; repo uses JPA. Keep JPA (do not migrate).
- [ ] `ConversationController` builds view data inline (`selectMailbox`, `scopeAddress`, `chatFor`); extract into a service before adding API controllers to avoid drift.

## Missing tests / verification
- [ ] `./mvnw test` not run in the Phase 0 run (see CHANGELOG).
- [ ] IMAP IDLE unverified against a real server (see CLAUDE.md roadmap).

## Process
- [ ] Routine says commit to `master`; this session's harness pins branches `claude/gifted-carson-tuzrgh` (backend) and `claude/trusting-cori-tuzrgh` (mobile). Work went to those; decide which is canonical.
- [ ] Existing CLAUDE.md roadmap items (support email, Google OAuth publish, Web Push, delivery states, …) remain open.
