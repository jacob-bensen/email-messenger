# Internal TODO (known gaps, tech debt, deferred work)

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
1. No REST API for the mobile client. All endpoints are HTML/session/CSRF; unauthenticated
   requests redirect to `/login`; errors render HTML. Need `/api/v1` (see `docs/API.md` Planned v1)
   with stateless bearer auth (new `SecurityFilterChain` for `/api/**`, CSRF off, JSON 401).
2. No token-auth mechanism (no JWT/API-token table). Needs Flyway `V36+` migration.
3. `GET /chats/events` SSE relies on the session cookie; client must handle or use a token query/header scheme.

## Logic drift (web/API must share a service layer)
4. `ConversationController` holds business orchestration: `parseRecipients`, `sendingAccount`,
   `withSignature`, send + `OutboundMessageService.record*` sequence, `scopeAddress`/`fallbackAccount`.
   Extract into a service (e.g. `ChatSendService`) before exposing via REST.
5. `ReplyService` always threads onto the thread's last message (roadmap item "Reply-to-a-specific-bubble").

## Missing tests / verification
6. Backend baseline `mvn -q -B test` (2026-10-29): exit 0, 794 tests, 0 failures, 0 errors, 1 skipped. No REST tests exist (no REST API).
7. IMAP IDLE unverified against a real server (see CLAUDE.md roadmap).

## Docs
8. `docs/API.md` DTO schemas TBD until records are defined. Event/DTO shapes for list items need extraction from `ConversationListItem`/`ChatConversation`.
9. Routine assumes jOOQ; backend uses JPA — update the routine prompt if jOOQ is not intended.
10. Branching: routine prompt says commit to `master`; the session pins feature branches
    (`claude/gifted-carson-hzucoy`, `claude/trusting-cori-hzucoy`). Work was pushed to those branches.

## Frontend (conexus-mail-mobile)
11. Entire app not started — see its `docs/DESIGN.md` §ROADMAP. Flutter/Dart SDK not installed in this environment.
