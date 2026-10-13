# Internal TODO (backend)

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
- No REST API exists for the mobile client; `docs/API.md` §PLANNED defines it. Blockers:
  1. Bearer-token auth (current auth is session cookie + CSRF only).
  2. JSON error envelope (currently HTML error pages / 302 to /login).
  3. Document SSE payload for `/chats/events` (schema TBD).

## Logic drift (web vs future API)
- `ConversationController.send/list/chat` holds orchestration (sending-account choice,
  signature append, reply-vs-new, recipients, mailbox scoping). Extract into a service so
  `/api/v1` and Thymeleaf share it. Do this as part of the first API feature.
- `ReplyService` always threads onto the thread's last message (see CLAUDE.md roadmap).

## Missing / unverified
- IMAP IDLE not verified against a real server (CLAUDE.md roadmap).
- Attachments are not downloadable (`blob_ref` null) — mobile needs this.
- Tests were not run in the Phase 0 session (docs-only change); see CHANGELOG.

## Style / tooling
- Generic routine assumes jOOQ + `master` branch; this repo uses JPA and the session
  branch. Branch rule in the routine conflicted with session instructions; session branch used.
- Stray `master/` directory holds older planning docs (PLAN/BACKLOG); consider reconciling.
- CI and Docker were intentionally removed (see CLAUDE.md); Dockerfile/docker-compose.yml
  still present in the tree.
