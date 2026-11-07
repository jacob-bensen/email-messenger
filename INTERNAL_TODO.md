# Internal TODO (gaps, debt, deferred)

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
1. No mobile-consumable API: no `/api/**`, no bearer/token auth, no JSON error
   envelope. Session+CSRF only. Blocks the entire mobile client. Plan in
   `docs/API.md` (PLANNED). First feature: token auth + read-only chat list.
2. SSE `new-mail` payload is now documented; keep-alive/heartbeat and reconnect semantics still TBD.
3. Attachments are not downloadable (`blob_ref` always null) — mobile cannot
   show files (also on CLAUDE.md roadmap).

## Logic drift (to prevent before adding REST)
4. Send orchestration lives in `ConversationController.send` (recipient
   resolution, sending-account choice, signature, asReply vs new email,
   outbound recording). Extract into a service so REST and web share it.
5. Account scoping (`scopeAddress`, `selectMailbox`, `sendingAccount`) is
   private controller code; extract with #4.
6. `ReplyService` always threads onto last message (see roadmap item).

## Tests
7. No tests exist for a REST surface (n/a until built).

## Docs / process
8. Routine prompt assumes jOOQ + `master`; repo uses JPA and the session
   mandates feature branches. Prompt should be updated.
9. Web Push, delivery states, etc.: see CLAUDE.md roadmap.
