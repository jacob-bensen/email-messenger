# Internal TODO (ordered by severity)

## Contract breakage
1. **No mobile API or token auth.** Server only exposes session+CSRF HTML. Design
   `docs/API.md` v1 (bearer/refresh tokens, `/api/v1/**`, CSRF-exempt) and have it
   call `ChatService`/`ConversationListService`/`ReplyService`/`MailAccountService`.
   Candidate first feature: login + conversation list + read a conversation.
2. Mobile repo is an empty scaffold (README only); no Flutter project yet.

## Logic drift
3. `ConversationController` builds list/pane models inline (`populateList`,
   `selectMailbox`, `scopeAddress`); extract into a service so an API can reuse
   mailbox-scoping rules rather than duplicating them.
4. Stack mismatch with routine assumptions: JPA, not jOOQ.

## Missing tests / docs
5. Document `ChatEventStream` SSE payload in `docs/API.md`.
6. Attachments aren't downloadable (`blob_ref` null) — blocks mobile file viewing.

## Environment
7. `flutter`/`dart` not installed in this sandbox.
8. Routine says work on `master`; this session was pinned to
   `claude/gifted-carson-bce0dl` / `claude/trusting-cori-bce0dl` and CLAUDE.md
   forbids pushing to main. Commits went to the session branches.
