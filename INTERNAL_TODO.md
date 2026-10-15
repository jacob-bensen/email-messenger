# INTERNAL_TODO (backend)

Ordered by severity (contract breakage > logic drift > missing tests > style).

## Contract breakage
- [ ] No JSON REST API and no token auth exist; the mobile client repo has no
      backend to talk to. Design `/api/v1` in `docs/API.md` first (auth token,
      chat list, chat detail, send), then implement over existing services.
- [ ] `GET /chats/events` SSE payload not documented in `docs/API.md`.

## Logic drift
- [ ] Web controllers build view models in `web` (`ChatService`, `ConversationListService`);
      before adding the API, extract DTO-producing methods so both share them.
- [ ] Routine assumes jOOQ; repo uses JPA. Keep JPA (CLAUDE.md governs).

## Missing tests / verification
- [ ] Not run this session: `./mvnw test` (see CHANGELOG for result).
- [ ] IMAP IDLE unverified against a real server (see CLAUDE.md roadmap).

## Process
- [ ] Sync routine says commit to `master`; the cloud session is pinned to
      `claude/gifted-carson-9zs56x`. Branch pinning followed; reconcile policy.
- [ ] Tracked directory `master/` (contains BACKLOG.md) in repo root — clarify its role vs CLAUDE.md roadmap.
