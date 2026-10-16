# INTERNAL_TODO (backend) — ordered by severity

## Contract breakage
- No REST API exists; mobile repo (`conexus-mail-mobile`) has nothing to call. Define auth scheme (bearer tokens, separate `SecurityFilterChain` for `/api/**`, CSRF off there) then implement `/api/v1` per `docs/API.md`. **Next run's feature.**
- `GET /chats/events` payload undocumented.

## Logic drift (before adding API)
- `ConversationController` embeds send logic (recipient parsing, `sendingAccount`, `withSignature`, key computation, `recordNewEmail`) and list-model assembly (`accountColorMap`, `fallbackAccount`). Extract into a service so web + API share it.

## Process / docs
- Routine prompt assumes jOOQ; project uses JPA. Treat jOOQ steps as N/A.
- Routine says commit to `master`; sessions are pinned to `claude/*` branches by the harness, so work landed there this run.
- Quality gates not run this run (docs-only change).

## Missing tests
- None identified beyond API (no API yet).
