# INTERNAL_TODO (ordered by severity)

## Contract breakage
- No JSON API or token auth exists; mobile repo (`conexus-mail-mobile`) has
  only a README. First feature: bearer-token auth + read-only chat list/detail
  (see `docs/API.md` "Planned").

## Logic drift
- None possible yet (no client). When adding the API, controllers must call the
  same services as the Thymeleaf controllers (`ChatService`, `ReplyService`).

## Process / environment
- Routine prompt assumes jOOQ and `master`-only commits; this repo uses JPA and
  sessions are pinned to `claude/*` branches. Routine follows the session's
  branch rule.
- `master/` dir at repo root is an unrelated routine's files; consider moving.
- `flutter`/`dart` not installed in the routine environment; frontend gates
  cannot run until they are.

## Missing tests
- SSE payload shape of `/chats/events` is undocumented/untested for clients.

## Style
- `docs/API.md` SSE payload TBD.
