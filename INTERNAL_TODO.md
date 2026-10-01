# Internal TODO (backend)

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
- No mobile REST API exists; `docs/API.md` "Planned" is a proposal only. Needs bearer-token auth
  (current auth is cookie+CSRF), JSON error envelope, and `/api/v1` conversations/send/mailboxes.
- `/chats/events` SSE payload schema undocumented.

## Logic drift
- None found yet (no second API path exists). When `/api/v1` is added, route through the existing
  services rather than the controllers.

## Missing tests
- TBD: audit test coverage for `/chats/refresh` JSON shape.

## Style / process
- Scheduled routine says "work on master, jOOQ"; this repo uses JPA and the session mandates a
  feature branch. Routine prompt should be updated to match.
