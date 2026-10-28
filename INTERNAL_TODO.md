# INTERNAL_TODO (ordered by severity)

## Contract breakage
- P0: No REST/JSON API and no bearer-token auth exist; the mobile client (`conexus-mail-mobile`) has nothing to call. Define `/api/v1` in `docs/API.md`, add token auth + CSRF-exempt `/api/**` filter chain, JSON error envelope. Suggested first feature: login + list chats + send message, reusing `ConversationListService`, `ChatService`, `OutboundMessageService`.
- `/chats/events` SSE payload shape undocumented (`ChatEventStream`).

## Logic drift
- None found between web and API (no API). Re-audit when the API lands: controllers in `web/ConversationController` still hold some orchestration (mailbox scoping, recipients) that should move to a service before reuse.

## Process / docs
- Generic routine assumes jOOQ; this repo uses JPA. Keep JPA.
- Two roadmap systems: `CLAUDE.md` roadmap (this repo's routine) and `master/PLAN.md|BACKLOG.md` (older operator routine). Reconcile.
- Branching: sync routine says master-only; this session's harness mandates `claude/gifted-carson-7q0b2r`. Worked on the session branch.
- `master/CHANGELOG.md` is ~500 KB; root `CHANGELOG.md` is the sync-routine log.

## Missing tests / style
- Not verified this run: full `mvn verify` (see CHANGELOG).
