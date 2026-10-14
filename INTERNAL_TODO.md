# INTERNAL_TODO (ordered by severity)

## Contract breakage
- [ ] No `/api/**` REST layer and no bearer-token auth: the mobile client cannot authenticate or read chats. Needs token auth (login/refresh), stateless `SecurityFilterChain` for `/api/**` (no CSRF), then `GET /api/v1/chats`, `/chats/{key}`, `POST …/messages` per `docs/API.md` Draft v1.
- [ ] SSE `/chats/events` payload and JSON error envelope undocumented/undefined.

## Logic drift
- [ ] Controllers (`ConversationController`) do mailbox selection / scope resolution (`selectMailbox`, `scopeAddress`) privately; extract into a service before a REST controller reuses it, so web and API can't diverge.

## Missing tests / tooling
- [ ] Routine assumes jOOQ + `mvn verify`; repo uses JPA. Routine prompt should be corrected.
- [ ] Frontend repo has no Flutter project (`flutter create .` needed); flutter/dart not installed in the sync environment.
- [ ] Attachment bytes not stored (`blob_ref` null) — mobile can't download attachments (also a CLAUDE.md roadmap item).

## Process
- [ ] Routine says commit to `master`; the cloud session is pinned to feature branches (`claude/gifted-carson-isfe0d`, `claude/trusting-cori-isfe0d`), and the backend CLAUDE.md forbids pushing to main. Work was committed to the session branches; decide whether to merge to master.
