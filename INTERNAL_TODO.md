# INTERNAL_TODO

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
- No `/api/v1` REST layer or token auth exists; the mobile client cannot authenticate or fetch chats. First slice:
  `POST /api/v1/auth/token` + `GET /api/v1/chats` (see `docs/API.md` §B). Needs a decision: stateless JWT vs. opaque
  tokens in a table.
- `GET /chats/events` SSE event name/payload shape is undocumented (read `ChatEventStream`) and CSRF/session-bound.
- `POST /chats/refresh` returns bare JSON but has no documented error shape.

## Logic drift
- Not audited yet: web controllers (`ConversationController`) build some view data inline; check that anything a
  future API needs lives in `ChatService`/`ReplyService`, not the controller.
- Target stack says jOOQ; code uses JPA. Treat JPA as authoritative; do not introduce jOOQ.

## Missing tests
- Flutter-side: none (client not scaffolded).

## Deferred / process
- Branch policy conflict: the routine prompt says commit to `master`, but this environment pins sessions to
  `claude/gifted-carson-htgf87` (here) and `claude/trusting-cori-htgf87` (mobile). Work happened on those branches.
- Prior-history docs live in `master/` (PLAN, BACKLOG, CHANGELOG, MASTER_ACTIONS); root `CHANGELOG.md` is new.
- Roadmap items remain in `CLAUDE.md` (support email, Web Push, delivery states, etc.).
