# Internal TODO (backend)

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
- No mobile REST API exists: auth is cookie+CSRF+HTML redirects. Needs
  `/api/v1/**` with token auth (e.g. login → bearer/refresh token), JSON
  errors (401 not 302), and DTOs for chats list, chat messages, send,
  mailboxes. Suggested first feature: auth + `GET /api/v1/chats` +
  `GET /api/v1/chats/{key}` + `POST /api/v1/chats/{key}/send`, backed by
  `ConversationListService`/`ReplyService`.
- `docs/API.md` only documents the 2 JSON-ish endpoints that exist.

## Logic drift
- Controllers (`ConversationController`) assemble view models inline
  (`populateList`, `populateChat`); extract to a service before reusing in
  an API controller.

## Docs/stack
- Generic routine assumes jOOQ; repo uses JPA. Keep docs saying so.
- Roadmap lives in `CLAUDE.md` (not `docs/DESIGN.md`, which belongs to the
  mobile repo).

## Verification
- IMAP IDLE not verified against a real server (see CLAUDE.md roadmap).
