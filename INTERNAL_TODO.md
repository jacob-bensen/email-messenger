# Internal TODO (backend)

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
- No machine API for `conexus-mail-mobile`: no token auth, no `/api/v1/**`, no DTOs. Client
  repo is an empty Flutter scaffold, so nothing is broken yet, but the first mobile feature
  needs this. Suggested first slice: `POST /api/v1/auth/token` + `GET /api/v1/chats` +
  `GET /api/v1/chats/{key}` backed by `ChatService`.
- `GET /chats/events` payload undocumented.

## Logic drift
- `ChatService`, `OutboundMessageService` and `ConversationController` are package-private in
  `web`; an API layer must reuse them (or move shared logic into `service`) rather than copy.
- Stack deviates from the routine's generic assumption (JPA, not jOOQ) — keep JPA.

## Missing tests / unverified
- `./mvnw test` not run in the Phase 0 session (docs only); see CHANGELOG.
- IMAP IDLE unverified against a real server (see CLAUDE.md roadmap).

## Housekeeping
- Repo contains a `master/` directory (`BACKLOG.md`, `CHANGELOG.md`, `MASTER_ACTIONS.md`) from an earlier
  routine; its CHANGELOG is separate from the root `CHANGELOG.md`. Decide whether to merge.
- Branching: this routine's prompt says commit to `master`; the session was pinned to feature
  branches, so work went to those instead.
