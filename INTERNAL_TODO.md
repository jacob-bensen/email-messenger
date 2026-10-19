# Internal TODO (backend)

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
1. **No mobile API.** No `/api/v1`, no bearer/token auth, session+CSRF only. The
   mobile client has nothing to call. Next run: define schemas in `docs/API.md`
   (auth token + chat list first), then implement controllers over existing services.
2. `/chats/events` SSE payload shape undocumented (TBD in API.md).

## Logic drift
3. Web controllers (`ConversationController`) bind HTTP params and build models
   directly; confirm all logic is in services before adding API controllers
   (audit not completed this run).

## Missing tests
4. None found yet for API (none exists). Verify test suite state: `./mvnw test`
   not run this run.

## Style / docs
5. Generic routine assumes jOOQ and `master`-branch commits; actual stack is JPA
   and this session is pinned to feature branches. Updated in `docs/BACKEND.md`.
6. Stray `master/` directory (PLAN/CHANGELOG/BACKLOG, 500KB changelog) is
   tracked; unclear if still used. Not touched.
7. Root `CHANGELOG.md` did not exist; created (old history is in `master/CHANGELOG.md`).
