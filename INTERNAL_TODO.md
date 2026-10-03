# INTERNAL_TODO

Ordered by severity (contract > logic drift > missing tests > style).

## Contract
- No REST API exists; mobile repo has no client to sync with. Define `/api/v1` in
  `docs/API.md` first, starting with conversations list/detail/send.
- No token auth (session cookie + CSRF only). Needed for mobile (Bearer token or
  API-scoped session).

## Logic drift
- `ConversationController` contains mailbox-scope selection helpers
  (`selectMailbox`, `scopeAddress`, `chatFor`) that a REST controller would need
  too — move into a service before adding the API.

## Doc/Stack drift
- Routine template assumes jOOQ; this repo uses JPA. Treat JPA as authoritative.

## Process
- Scheduled routine says "master only"; the cloud session designates feature
  branches (`claude/gifted-carson-fkpyuq` backend, `claude/trusting-cori-fkpyuq`
  mobile). Branches were used; reconcile the routine prompt.
- Quality gate `mvn -q -B verify` not yet run in this environment (see CHANGELOG).
