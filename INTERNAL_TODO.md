# INTERNAL_TODO (ordered by severity)

## Contract breakage
- No mobile REST API and no token auth exist; `conexus-mail-mobile` has nothing to call.
  Define `docs/API.md` v1 (auth, chat list, chat detail, send) then implement under `/api/v1/**`
  with a stateless security chain (session+CSRF stays for the website).

## Logic drift (blockers for adding an API)
- `ConversationController` holds business logic inline (recipient parsing, signature
  appending, sender-mailbox selection, conversationKey computation, attachment handling).
  Extract into a service so web and API share one path.

## Missing tests / verification
- Backend suite not run in the 2026-09-30 sync session (see CHANGELOG).
- SSE event payload undocumented; IDLE not verified against a real IMAP server (CLAUDE.md).

## Style / process
- Routine prompt assumes jOOQ and `master`; repo uses JPA and the session branch
  `claude/gifted-carson-finpz8`. Existing operational logs live in `master/` (CHANGELOG, PLAN, BACKLOG).
- Attachments are not downloadable (`blob_ref` null) — the mobile client will need this.
