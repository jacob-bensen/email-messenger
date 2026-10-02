# INTERNAL_TODO

Ordered by severity: contract breakage > logic drift > missing tests > style.
Last updated 2026-10-02 (first sync audit; mobile repo is an empty stub).

## Contract breakage
1. **No mobile API or token auth exists.** Everything but 3 endpoints is
   server-rendered HTML behind session + CSRF. Build `/api/v1/auth/*` (bearer
   tokens) and the chats read endpoints from `docs/API.md` §2. Needs a decision
   on token format (JWT vs opaque, stored hashed) — unblocks the mobile repo.
2. `docs/API.md` §1 SSE event name/payload and webhook error codes are TBD;
   read `ChatEventStream` / `BillingWebhookController` and pin them.

## Logic drift / architecture
3. `ConversationController` holds mailbox selection (`selectMailbox`,
   `scopeAddress`, `accountColorMap`, `fallbackAccount`) — extract to a service
   so `/api/v1` and web share it.
4. Task template assumes jOOQ; repo uses JPA. Docs reflect JPA. No action
   unless the project decides to migrate.
5. `Attachment.blob_ref` is always null: attachments are not downloadable
   (also a roadmap item in `CLAUDE.md`). Mobile needs a download endpoint.

## Missing tests
6. No API-level (MockMvc JSON) tests exist because no API exists.

## Process / style
7. Repo has its own planning docs in `master/` (PLAN/BACKLOG/CHANGELOG) that
   predate this routine; root `CHANGELOG.md` is the routine's log. Consider
   consolidating.
8. Routine prompt says commit to `master`; this session's harness designates
   branch `claude/gifted-carson-6v93xa` (and `CLAUDE.md` forbids pushing to
   main). Work was pushed to the designated branch.
9. Docker/CI files exist (`Dockerfile`, `.github/workflows/ci.yml`) although
   `CLAUDE.md` says they were removed — verify and reconcile.
