# Internal TODO (backend)

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
- [ ] No `/api/v1` REST API and no token auth exists; the mobile client has nothing to call.
      Define it in `docs/API.md`, add stateless auth (Bearer) alongside the cookie session.
- [ ] `/chats/events` SSE payload shape undocumented precisely (verify `ChatEventStream`).

## Logic drift
- [ ] Chat logic lives partly in controllers (`ConversationController.send` validates
      body/attachments inline). Extract to a service before an API reuses it.
- [ ] Attachments are metadata only (`blob_ref` null) — mobile can't download them.

## Missing tests / docs
- [ ] Not run this session: `mvn -q -B verify` (see CHANGELOG).

## Housekeeping / style
- [ ] Routine prompt assumes jOOQ; repo uses JPA. Treat JPA as authoritative.
- [ ] Stray `master/` directory (BACKLOG/CHANGELOG/PLAN) predates the doc system; reconcile or remove.
- [ ] Routine prompt says commit to `master`; this session was restricted to branch
      `claude/gifted-carson-qwkjgm`, and `CLAUDE.md` says not to push to main.
- [ ] Existing `CLAUDE.md` roadmap is the product roadmap; keep it as the backlog source.
