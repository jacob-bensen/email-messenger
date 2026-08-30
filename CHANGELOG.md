# CHANGELOG — email-messenger

Append a dated entry each sync-routine run. Older git commits are the
authoritative record before this file existed.

## 2026-08-30 — Phase 0 doc bootstrap

- Created `docs/BACKEND.md` (architecture snapshot: stack, package
  layout, controller → service → table map, auth model, real-time
  path, config).
- Created `docs/API.md` — canonical REST contract. Recorded honestly
  that **no REST API exists yet**; laid out the bootstrap order
  (mobile-auth → chats-list → chat-pane → send → SSE) and the
  conventions (bearer tokens, RFC 3339 timestamps, cursor pagination,
  error envelope, `Idempotency-Key`, CSRF scoping).
- Created `docs/WEB.md` — full Thymeleaf page inventory keyed to
  controllers, and an anti-drift checklist for page changes.
- Created `INTERNAL_TODO.md` — severity-ordered punch-list.
- Updated `CLAUDE.md` with a "Related repositories" section pointing
  at `conexus-mail-mobile`.
- Notes: uses **JPA** (not jOOQ, as the umbrella routine assumed) —
  follow JPA idiom already in the codebase.
- Branching: this session is bound by the harness to
  `claude/relaxed-newton-jid7nr` (NOT `master`); committed and pushed
  there.
