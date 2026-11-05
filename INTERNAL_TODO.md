# INTERNAL_TODO

Ordered by severity (contract breakage > logic drift > missing tests > style).

## Contract breakage
- No `/api/v1` REST layer or token auth exists; the mobile client cannot talk to
  the server. First feature run: bearer login + read-only `GET /api/v1/chats`
  and `/api/v1/chats/{key}`; then update `docs/API.md` to "implemented".

## Logic drift
- `ConversationController` embeds list-filter and recipient parsing and calls
  `MailboxPollingService`/`ConversationKeyService` directly; extract into
  `ChatService`/`ConversationListService` so web and API share it.
- Routine template assumes jOOQ; repo uses JPA. Keep following JPA.

## Missing tests
- None audited this run (no code changed).

## Process
- Routine says "commit to master"; this session's harness mandates branch
  `claude/gifted-carson-4bkpvq`, and CLAUDE.md forbids pushing to main. Followed
  the branch rules; merge to master is a human step.
