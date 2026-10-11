# INTERNAL_TODO (backend)
Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
1. No mobile API/auth: client cannot authenticate (session + CSRF only). Design `/api/v1/**` with bearer tokens (needs a separate stateless SecurityFilterChain) — define in docs/API.md first.
2. No JSON DTOs for conversations/messages; only HTML views exist.
3. Errors are HTML (`error.html`); define a JSON error envelope for `/api/**`.

## Logic drift
4. `ConversationController.send` orchestrates reply-vs-new-email, signature, recipients, attachments inline. Extract to a service (e.g. `ChatService.send`) before exposing via REST so web and API share one path.
5. `ReplyService` always threads onto the thread's last message (see CLAUDE.md roadmap: reply-to-bubble).

## Missing tests
6. Docs claim nothing about API tests yet; add MockMvc tests when `/api/v1` lands.
7. Quality gates not run in the doc-bootstrap run (see CHANGELOG 2026-10-11).

## Style / docs
8. Persistence is JPA, not jOOQ as the sync routine assumes — keep docs/BACKEND.md note.
9. Request fields for `/register` and `/chats/new` are TBD in docs/API.md.
10. Branching: the routine says commit to `master`; this session is pinned to feature branches by the harness, and CLAUDE.md forbids pushing to main. Commits go to the session branch.
