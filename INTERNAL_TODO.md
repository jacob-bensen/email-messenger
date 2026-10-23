# INTERNAL_TODO (backend)

Ordered by severity.

## Contract breakage
- No REST API exists for the mobile client; no bearer-token auth (`SecurityConfig` is cookie+CSRF only). Blocks any client feature. Design `/api/v1` in docs/API.md first (auth + conversation list/detail + send).

## Logic drift
- Task template assumes jOOQ; repo uses JPA. Treat as intentional; do not introduce jOOQ.
- Branch policy conflict: routine prompt says `master`, session config mandates `claude/gifted-carson-9hee31`. Work landed on the session branch.

## Missing tests / verification
- Quality gate not run this run (see CHANGELOG 2026-10-23).
- IMAP IDLE unverified against a real server (CLAUDE.md roadmap).

## Style / docs
- Attachments not downloadable (`blob_ref` null) — needed before mobile can show files.
