# Internal TODO

Ordered by severity: contract breakage > logic drift > missing tests > style.
Last updated 2026-10-30 (sync audit, first run).

## Contract breakage
1. **No mobile API or token auth.** The server only offers cookie-session HTML
   (CSRF on). `docs/API.md` §2 lists the planned `/api/v1` surface. Next run:
   token auth + `GET /api/v1/chats` + `GET /api/v1/chats/{key}` on top of
   `ConversationListService` / `ChatService`, with a JSON error envelope.
2. **Frontend repo is empty** (`README.md` only): no Flutter project, no
   `lib/domain/`. Nothing calls the server yet, so there is no client/server
   drift to fix, only a project to scaffold.

## Logic drift
3. `ConversationController.send`/`createMessage` orchestrate recipient parsing,
   signature append, reply-vs-new branching and outbound recording inline. Move
   into a service (e.g. `ChatSendService`) so web and API share it before
   adding a send endpoint.
4. Target stack says jOOQ; the code uses JPA/Hibernate. Decision needed:
   keep JPA (recommended; rewriting 35 migrations' worth of entities is out of
   scope) and update the routine's stack assumption. No jOOQ codegen exists.

## Missing tests / verification
5. `mvn -q -B test` passed on 2026-10-30 (exit 0). Not run: `verify`
   (no integration profile checked) and any frontend gate.
6. IMAP IDLE (live delivery part 1) never verified against a real server
   (see CLAUDE.md roadmap).

## Product gaps (from CLAUDE.md roadmap)
7. Attachments are not downloadable (`blob_ref` always null).
8. Web Push, delivery states, reply-to-specific-bubble, signature contact
   cards, support email, Google OAuth consent publish.

## Process note
The routine asks for commits to `master`; this session is restricted to the
feature branch `claude/gifted-carson-k3qeit` (backend) and
`claude/trusting-cori-k3qeit` (frontend). Work was committed there.
