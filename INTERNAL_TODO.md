# INTERNAL_TODO (ordered by severity)

## Contract breakage
- No REST API for the mobile client; no token auth. Client cannot function. Design `/api/v1`
  in `docs/API.md` first (auth, conversations list/detail, send, mailboxes, plan limits).
- `/chats/refresh` and `/chats/events` depend on session cookies + CSRF; define mobile-safe equivalents.

## Logic drift
- None found (no second code path exists yet). When `/api/v1` is added it must reuse `ChatService`,
  `ConversationListService`, `ReplyService`, `OutboundMessageService`.

## Missing tests
- Attachments are not stored (`blob_ref` null) so no download test/endpoint exists.

## Style / housekeeping
- Repo contains a stray `master/` directory (BACKLOG/CHANGELOG/MASTER_ACTIONS/PLAN from an earlier
  routine, ~500KB changelog). Decide whether to keep; this file's `CHANGELOG.md` at root is new.
- Generic routine assumes jOOQ; this repo uses JPA. Docs reflect reality.
- Routine says commit to `master`; this session is pinned to branch `claude/gifted-carson-xq1atb`
  and CLAUDE.md forbids pushing to main. Work was done on the pinned branch.
- Pre-existing roadmap items remain in `CLAUDE.md` (support email, OAuth consent, Web Push, delivery states, ...).
