# INTERNAL_TODO

Severity order: contract breakage > logic drift > missing tests > style.

## Contract breakage
- No REST/JSON API and no token auth exist; the mobile repo (`conexus-mail-mobile`)
  has no code. Define `docs/API.md` §2 first, then implement `/api/v1/**` over the
  existing services.
- `GET /chats/events` SSE payload is undocumented (see `docs/API.md`).

## Logic drift
- `ConversationController` injects `EmailThreadRepository` directly and holds
  chat-scoping/selection logic (`selectMailbox`, `scopeAddress`, `chatFor`). Move
  into a service before an API layer duplicates it.
- Attachments are not downloadable (`blob_ref` null) — blocks a mobile client from
  showing files (see CLAUDE.md roadmap).

## Missing tests
- None audited this run.

## Process notes
- The sync routine assumes jOOQ and `master`-only work; this repo uses JPA and the
  session instructions designate feature branches. Routine adapted accordingly.
- Java/Maven build not run this run (docs-only change).
