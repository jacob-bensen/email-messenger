# INTERNAL_TODO (ordered by severity)

## Contract breakage
1. **No REST API and no token auth.** The mobile client has nothing to call; session+CSRF only. Design `/api/v1` auth (bearer + refresh), implement the read-only slice in `docs/API.md` §2 first.

## Logic drift / design debt
2. `ConversationController.send` and `.createMessage` duplicate the send flow (recipient parsing, `sendingAccount`, `withSignature`, `replyService.send*` + `outboundMessageService.record*`) in the controller and use repositories directly. Extract a `ChatSendService` before adding an API send endpoint so web and API share it.
3. `ChatService`/`ConversationListService` are package-private in `web`; widen or move to `service` for API controllers.
4. Routine prompt assumes jOOQ + `master`; repo uses JPA and session branches. Update the routine prompt.

## Missing tests / verification
5. (done 2026-11-07) Baseline `mvn -q -B verify` passed, exit 0.
6. Roadmap items in CLAUDE.md remain (support email, OAuth consent publish, Web Push, delivery states, signature contact card, shared files, reply-to-bubble). IMAP IDLE untested vs a real server.

## Style
7. `/chats/events` payload undocumented — document in API.md.
