# INTERNAL_TODO (backend) — ordered by severity

## Contract breakage
- [ ] No JSON API or token auth exists; `conexus-mail-mobile` cannot talk to the server. Implement `docs/API.md` §Proposed v1 (login+token filter chain, `/me`, `/chats`, `/chats/{key}`).
- [ ] Attachments are not downloadable (`blob_ref` always null) — mobile cannot show files (roadmap item in CLAUDE.md).

## Logic drift / layering
- [ ] Send orchestration (reply-vs-new, signature, recipients, `OutboundMessageService.record*`) lives in `ConversationController`; extract to a service so web and API share it.
- [ ] `ReplyService` always threads onto the thread's last message (roadmap: reply-to-bubble).

## Spec deviations to confirm with owner
- Routine assumes jOOQ + `master` branch; repo uses JPA and the session was pinned to branch `claude/gifted-carson-6edmg6` (CLAUDE.md also says "do not push to main"). Work was done on the pinned branch.
- Root `CHANGELOG.md` is new; legacy ledger is `master/CHANGELOG.md` (3.5k lines, EPIC-based).

## Missing tests / tooling
- [ ] Gates not run on 2026-10-16 doc-only run (see CHANGELOG).
