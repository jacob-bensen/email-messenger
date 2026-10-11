# INTERNAL_TODO

Ordered by severity (contract breakage > logic drift > missing tests > style).

## Contract breakage
- No `/api/v1` JSON API and no token auth; the mobile client cannot authenticate
  (session + CSRF + HTML only). Plan in `docs/API.md` §B. Next feature.
- `/chats/events` SSE payload shape undocumented.

## Logic drift
- None found: controllers delegate to services. Re-audit when `/api/v1` lands;
  `ConversationController` holds `selectMailbox`/`scopeAddress` helpers an API
  controller would need — extract to a service first.

## Missing tests / gaps
- Attachments not downloadable (`blob_ref` null) — blocks mobile attachment UX.
- Quality gates not run in the Phase 0 session (docs-only).
- Roadmap items in `CLAUDE.md` (support email, OAuth consent publish, Web Push…).

## Style
- Routine assumed jOOQ; project uses JPA — docs now say so.
