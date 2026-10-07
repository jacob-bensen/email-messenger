# Internal TODO (backend)

Ordered by severity (contract breakage > logic drift > missing tests > style).

## Contract breakage
- No mobile REST API or bearer-token auth exists; `conexus-mail-mobile` has no code to sync against. Implement `docs/API.md` §3 slice 1 (auth) first.
- Attachments not downloadable (`blob_ref` always null) — blocks mobile file viewing.

## Logic drift
- `ReplyService` always threads onto last message (see CLAUDE.md roadmap) — mobile reply UX will inherit this.
- CLAUDE.md/task template assume jOOQ; project actually uses JPA. Decide whether to keep JPA (recommended; no jOOQ migration planned).

## Missing
- `/chats/events` SSE event schema undocumented.
- No JSON error envelope.
- Phase 0 audit not yet run against tests/coverage: **TBD**.

## Process
- Scheduled routine says "commit to master"; session harness pins `claude/gifted-carson-3ym5hi` and CLAUDE.md forbids pushing to main. Branch rule followed: harness/CLAUDE.md. Confirm desired policy.
