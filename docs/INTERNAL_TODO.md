# INTERNAL_TODO.md (backend)

Ordered by severity.

## Contract breakage
1. No REST API / token auth for the mobile client (`docs/API.md` §Planned). Mobile repo has no code, so nothing is broken yet, but no feature can be built end-to-end until `/api/v1` + non-cookie auth exist. Next run: spec auth + read-only conversations in API.md, then implement.
2. No JSON error envelope; unauthenticated requests redirect to `/login`.

## Logic drift (web vs service layer)
3. `ConversationController.createMessage/send` orchestrate send flow inline (recipient parsing, sender selection `sendingAccount`, signature `withSignature`, key computation, thread choice, outbound recording). Extract into a service (e.g. `ChatSendService`) before any API endpoint reuses it, so web and API share one path.
4. Mailbox selection/scope helpers (`selectMailbox`, `scopeAddress`, `chatFor`) live in the controller; same extraction needed for the API.
5. `MailboxController.createMailbox` redirects to `/threads` (now a redirect shim to `/chats`) — cosmetic.

## Missing tests / verification
6. Test suite not run in the doc-bootstrap pass (see CHANGELOG).
7. IMAP IDLE unverified against a real server (from CLAUDE.md roadmap).

## Docs
8. API.md `/chats/events` payload shape TBD.
9. Roadmap in CLAUDE.md still has open items (support email, Google OAuth publish, Web Push, delivery states, signature card, shared files, reply-to-bubble).

## Process
10. Routine prompt assumes jOOQ and `master`; repo uses JPA and the session mandates a `claude/*` branch. CLAUDE.md says never push to main.
