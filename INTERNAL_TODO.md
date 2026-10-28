# INTERNAL_TODO (ordered by severity)

## Contract breakage
- No `/api/v1` exists; mobile client cannot talk to the server. Needs bearer-token auth (current auth = session cookie + CSRF), JSON controllers. Design in `docs/API.md` §2.
## Logic drift risk
- `ConversationController` holds orchestration (mailbox scoping, signature append, new-vs-reply, plan limits) outside services. Extract e.g. `ChatSendService` BEFORE adding API controllers so web and API share it.
- `ReplyService` always threads onto last message (see CLAUDE.md roadmap: reply-to-bubble).
## Missing pieces
- Attachments not downloadable (`blob_ref` null) — needed by mobile too.
- `/chats/events` SSE payload undocumented.
## Docs/style
- Routine template assumes jOOQ; backend uses JPA. Treat as JPA.
- `Dockerfile`/`docker-compose.yml` exist though CLAUDE.md says Docker was removed 2026-06-11; owner to decide (untouched).
- Backend dir `master/` is a docs folder, not a branch.
- Branching: routine says commit to `master`; the session mandates `claude/*` branches and CLAUDE.md forbids pushing main. Branches used; merge is owner's call.
