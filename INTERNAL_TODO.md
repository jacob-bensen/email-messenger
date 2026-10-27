# Internal TODO (ordered by severity)

## Contract breakage
- No `/api/v1` and no token auth: mobile client cannot log in or read data (form login + CSRF + cookies only). Define in `docs/API.md`, then implement a separate stateless `SecurityFilterChain` for `/api/**`.

## Logic drift / architecture
- `ConversationController` holds send/account-selection orchestration (`selectMailbox`, `sendingAccount`, `withSignature`, thread choice) — extract into a service so a REST controller shares it.
- Target stack says jOOQ; code uses JPA. Decide: keep JPA (recommended, update the routine's assumption) — nothing to do in code.
- `docs/API.md`: SSE payload schema of `/chats/events` undocumented (TBD).

## Security / hygiene
- `SecurityConfig` remember-me key is hard-coded (`conexusmail-remember-me`); move to config/env.
- `master/` directory in repo root holds another routine's PLAN/BACKLOG/CHANGELOG/MASTER_ACTIONS; and `Dockerfile`, `docker-compose.yml`, `.github/` exist although `CLAUDE.md` says Docker/CI were removed. Confirm with owner before deleting.

## Missing tests
- No API tests (no API). Baseline `./mvnw test` result recorded in `CHANGELOG.md`.

## Deferred (from CLAUDE.md roadmap)
Support email, Google OAuth consent publish, Web Push, delivery states, signature contact card, shared files, reply-to-bubble.
