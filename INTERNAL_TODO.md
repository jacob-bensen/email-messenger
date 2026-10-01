# INTERNAL_TODO (backend)

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
1. **No mobile API / token auth.** Only session+CSRF web routes exist; the Flutter client has nothing to call.
   Decide auth scheme (see docs/API.md §B), add `/api/v1` controllers that reuse `ChatService`,
   `ConversationListService`, `ReplyService`, `OutboundMessageService`, `MailboxPollingService`.
2. SSE event name/payload at `/chats/events` is undocumented — read `ChatEventStream` and pin it in API.md.
3. No JSON error envelope; `GlobalExceptionHandler` renders HTML.

## Logic drift
4. `ConversationController` holds scope/sender-account selection logic (`scopeAddress`, `sendingAccount`,
   `selectMailbox`) as private statics. Extract into a service before an API reuses it, or the two will diverge.
5. Docs assumed jOOQ; project uses JPA. Generic routine text should be adjusted.

## Missing tests / docs
6. API.md §A SSE and webhook details are partial ("TBD").
7. Backend `CLAUDE.md` roadmap (support email, OAuth publish, Web Push, delivery states, ...) is separate from
   this routine; historic changelog lives in `master/CHANGELOG.md` (3.5k lines).
