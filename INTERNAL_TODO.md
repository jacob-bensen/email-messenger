# INTERNAL_TODO (backend) — ordered by severity

## Contract breakage
1. **No mobile JSON API.** Client needs token auth + `/api/v1` (see `docs/API.md`). CSRF + cookie sessions block native clients. Highest-value next feature: `POST /api/v1/auth/login`, `GET /api/v1/me`, `GET /api/v1/chats` — thin controllers over `ConversationService`/`UserService`.
2. `/chats/events` SSE is cookie-session only.

## Logic drift
3. Routine assumes jOOQ; backend uses JPA/Hibernate. Docs/CLAUDE.md now say so; do not introduce jOOQ.
4. Controllers `ConversationController` (~300+ lines) may hold compose/send validation — audit before exposing via API so both paths share one service.

## Missing tests / verification
5. IMAP IDLE unverified against a real server (see CLAUDE.md roadmap).
6. Quality gates not run in the doc-bootstrap run (2026-11-03).

## Deferred / other
7. Roadmap items in `CLAUDE.md` (support email, Google OAuth publish, Web Push, delivery states, ...).
8. `master/` directory holds historical PLAN/BACKLOG/CHANGELOG (504KB); root `CHANGELOG.md` is the routine's running log.
