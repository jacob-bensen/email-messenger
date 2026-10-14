# Internal TODO (backend), ordered by severity

## Contract breakage
- No `/api/v1` exists; `docs/API.md` §2 is PROPOSED only. Mobile client (separate repo) has no code yet.
- No token auth: all non-public routes need session cookie + CSRF. Decide bearer vs. session for mobile.
- `/chats/events` SSE event schema undocumented in API.md (TBD).

## Logic drift risk
- `ConversationController` holds orchestration (mailbox scoping `selectMailbox`/`scopeAddress`, send validation, `markRead`+`chatFor`) that a REST controller would otherwise duplicate. Extract into a service before adding `/api/v1`.
- Stack deviation: JPA, not jOOQ — generic routine assumes jOOQ; docs record JPA as truth.

## Missing tests / verification
- (resolved) `mvn -q -B test` passes online; offline (`-o`) fails on empty ~/.m2.
- IMAP IDLE unverified against a real server (see CLAUDE.md roadmap).

## Deferred / roadmap
- See CLAUDE.md roadmap (Web Push, delivery states, contact card, attachments blob store, reply-to-bubble). Attachments are not downloadable (`blob_ref` null) — blocks mobile attachment viewing.
- Branch policy conflict: routine says commit to `master`; this session is pinned to `claude/gifted-carson-647s3n` and CLAUDE.md forbids pushing `main`. Work was committed on the session branch.
- Stray `master/` directory (old BACKLOG/PLAN/CHANGELOG/MASTER_ACTIONS from a prior routine) — decide whether to archive.
