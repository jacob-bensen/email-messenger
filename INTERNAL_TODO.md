# Internal TODO

Ordered by severity (contract breakage > logic drift > missing tests > style).

## Contract breakage
- [ ] No `/api/v1` REST surface and no token auth: the mobile client cannot log in or read conversations. Specify in `docs/API.md` first, then implement over existing services.
- [ ] `/chats/events` SSE event names/payload undocumented (see `ChatEventStream`).

## Logic drift
- [ ] `ConversationController.send` assembles recipients/validation inline; extract to a service before a REST send endpoint is added so web and API share it.
- [ ] Routine spec assumes jOOQ; backend uses JPA. Keep JPA (follow existing code); update routine assumptions.

## Missing tests
- [ ] None audited yet for `/chats/refresh` JSON shape.

## Process
- [ ] Routine says commit to `master`; this session is restricted to branch `claude/gifted-carson-k40r5l` (and `claude/trusting-cori-k40r5l` in the mobile repo). Work was pushed there; merge to master is manual.
