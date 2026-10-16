# INTERNAL_TODO

Ordered by severity (contract breakage > logic drift > missing tests > style). Updated 2026-10-16.

## Contract breakage
1. **No REST/token API exists** — mobile client cannot authenticate or fetch chats. Needs bearer-token auth, `/api/v1` per docs/API.md "Proposed", JSON error envelope. (Next feature run: start with `GET /api/v1/me` + token auth, then chat list.)
2. **Mobile repo is empty** (README only; no Flutter project, no `lib/domain`, no docs). Needs `flutter create`, DESIGN.md, MOBILE.md.
3. SSE `new-mail` payload is undocumented (read `ChatEventStream`).

## Logic drift
4. `ConversationController` private helpers (`selectMailbox`, `scopeAddress`, `chatFor`, `populateList`) contain mailbox-scope logic; extract to a service before adding REST controllers (otherwise web/API fork).
5. Routine prompt assumes jOOQ + `master` branch; actual stack is JPA and this environment mandates per-repo `claude/*` branches. Prompt assumptions to reconcile.

## Missing tests
6. Not yet audited for REST (none exist).

## Style / process
7. `master/` directory (PLAN/BACKLOG/CHANGELOG ~500KB from an older routine) overlaps root CHANGELOG.md creation; decide whether to merge/archive. Root `CHANGELOG.md` created fresh this run.
8. CLAUDE.md "Commands" says "once scaffolded" — stale; project builds.
