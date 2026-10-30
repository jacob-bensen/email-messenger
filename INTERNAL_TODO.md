# Internal TODO

Ordered by severity (contract breakage > logic drift > missing tests > style).

## Contract breakage
1. **No REST API or token auth for the mobile client.** Everything is
   session+CSRF HTML. Build `/api/v1` (bearer tokens, CSRF-exempt chain, JSON
   DTOs) over the existing services; first slice = login + chat list + chat
   open + send. Write schemas in `docs/API.md` first. (Recommended next run.)
2. `GET /chats/events` payload shape is undocumented; define it before the
   client consumes it.
3. No JSON error envelope (only `error.html`).

## Logic drift
4. `ConversationController` (~300+ lines) mixes request parsing with list/model
   assembly (`populateList`, filter parsing, mailbox selection). Extract the
   reusable parts into services before an API controller needs them, to avoid
   forking logic.
5. Mobile spec says jOOQ; backend uses JPA. Keep JPA (do not migrate).

## Missing tests
6. Not run this session (docs-only run): `mvn -q -B verify`.

## Style / process
7. Task prompt says work on `master`; this environment mandates feature
   branches (`claude/gifted-carson-de600m`). Branch policy needs reconciling.
8. Roadmap items remain in `CLAUDE.md` (support email, Web Push, delivery
   states, etc.).
