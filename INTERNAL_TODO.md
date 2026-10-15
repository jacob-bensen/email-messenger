# INTERNAL_TODO (backend)

Ordered by severity: contract breakage > logic drift > missing tests > style. Audit date 2026-10-15.

## P0 — contract breakage
- No JSON API or token auth exists; the mobile client has nothing to call. Next run: pick the smallest vertical slice (suggest: token login + `GET /api/v1/conversations`), write it in `docs/API.md` first.
- Extract mailbox-scope/filter orchestration out of `ConversationController` (`selectMailbox`, `scopeAddress`, `parseFilter`, `populateList`) into a service so an API controller shares it.

## P1 — logic drift / spec mismatches
- Routine prompt assumes jOOQ; the project uses Spring Data JPA. Do not introduce jOOQ without a decision (record in CLAUDE.md).
- Routine prompt says commit to `master`; session rules and `CLAUDE.md` say work on the designated branch, never push main. Branch rules win.
- `master/` directory in repo root holds the older PLAN/BACKLOG/CHANGELOG routine files (CHANGELOG ~500KB). Root `CHANGELOG.md` is this routine's; decide whether to merge.
- `docs/API.md` SSE event schema TBD.

## P2 — missing tests / unverified
- Quality gates not run this session (see CHANGELOG 2026-10-15).
- IMAP IDLE unverified against a real server (see CLAUDE.md roadmap).
- Attachments not downloadable (`blob_ref` null) — mobile will need it.

## P3 — style
- (none recorded)
