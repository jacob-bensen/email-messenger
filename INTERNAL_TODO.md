# INTERNAL_TODO (backend)
Ordered by severity (contract breakage > logic drift > missing tests > style).

## Contract breakage
- No `/api/v1` JSON API and no token auth (cookie+CSRF only) → mobile client cannot talk to the server. First feature should be: token login + `GET /api/v1/chats` + `GET /api/v1/chats/{key}`, specified in `docs/API.md` first.
- Orchestration prompt assumes jOOQ; the backend uses JPA/Flyway. jOOQ is NOT to be added; treat JPA as authoritative.
## Logic drift
- `ConversationController` builds list/pane/send flows with view-model population inline (sendingAccount, signature, recipient selection). Extract to a service before exposing via JSON to avoid web/API divergence.
## Missing / stale
- Root `CHANGELOG.md` was missing; the historical log lives in `master/CHANGELOG.md` (500 KB) alongside `master/{BACKLOG,PLAN,MASTER_ACTIONS}.md`. Decide whether to move `master/` docs to `docs/`.
- `CLAUDE.md` Tech-stack section and README are stale vs. reality (Security, Stripe, oauth2-client present).
- Flutter/Dart absent in this sandbox; mobile gates cannot run here.
