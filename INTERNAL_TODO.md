# INTERNAL_TODO (backend)

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage / blockers
- [ ] **No REST API or token auth for the mobile client.** `docs/API.md` has no mobile endpoints; security is session+CSRF only. Next feature run: design `/api/v1` (auth token, conversations list/detail, send) in API.md FIRST, then implement.
- [ ] SSE `/chats/events` payload shape is undocumented; document after reading `ChatEventStream`/`NewMailEvent`.

## Logic drift risk
- [ ] `ConversationController` holds business logic (recipient parsing, sending-account selection, signature append, mailbox scope) — extract into a service so a REST controller shares it (required by routine guardrail).
- [ ] Attachments are not downloadable (`blob_ref` null) — mobile would need this (roadmap item in CLAUDE.md).

## Missing / unverified
- [ ] `./mvnw` cannot download Maven in this sandbox (use system `mvn`). Testcontainers tests can't find Docker here, so they don't run in this environment.
- [ ] Routine template assumes jOOQ; repo uses JPA. Template stack assumption should be corrected in the routine prompt.
- [ ] Routine says work on `master`; this session was bound to branch `claude/gifted-carson-x3auqy` (and `claude/trusting-cori-x3auqy` for the mobile repo) and pushed there instead. Merge to master is a human step.
