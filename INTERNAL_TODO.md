# INTERNAL_TODO (cross-repo; ordered by severity)

## Contract breakage
- [ ] No JSON API or token auth exists; sibling repo `conexus-mail-mobile` has no code and
      cannot talk to the server. Define `/api/v1` in docs/API.md first, then implement.
- [ ] `/chats/events` SSE payload schema undocumented (read `ChatEventStream`/`NewMailEvent`).

## Logic drift risk
- [ ] Extract send/signature/recipient orchestration out of `ConversationController`
      (`sendingAccount`, `withSignature`, `parseRecipients`, `sendNewEmail`+`recordNewEmail`)
      into a service so web and API share it.
- [ ] Attachments are not downloadable (`blob_ref` null) — needed before the mobile client can show files.

## Missing tests
- [ ] Not audited this run (backend tests not executed; see CHANGELOG).

## Style / process
- [ ] Routine template assumes jOOQ and `master`; repo uses JPA and session branches
      (`claude/...`). Reconcile the routine prompt.
- [ ] Existing roadmap lives in CLAUDE.md; mobile roadmap in the sibling repo's docs/DESIGN.md.
