# INTERNAL_TODO

Ordered by severity (contract breakage > logic drift > missing tests > style). Added 2026-10-09 (sync audit).

## Contract breakage / blockers
1. **No mobile REST API or token auth.** Backend exposes only HTML (+ `/chats/refresh`, SSE, Stripe webhook).
   Mobile repo has no code. First feature: define `/api/v1` auth + read-only conversations list/detail in
   `docs/API.md`, implement over existing `ConversationListService`/`ChatService`, then the Flutter client.
2. Session-cookie + CSRF is the only auth; need bearer/refresh-token scheme (new migration V36+).
3. Attachments are not downloadable (`blob_ref` null) — mobile cannot show files (see CLAUDE.md roadmap).

## Logic drift / structure
4. Routine assumes jOOQ; repo uses JPA. Don't introduce jOOQ — follow JPA.
5. `ConversationController` builds model attributes inline (`populateList`); extract view-model DTOs usable by
   both Thymeleaf and a future JSON API so logic isn't forked.
6. Branching: routine prompt says `master`; this checkout works on the session-assigned feature branch
   (`claude/gifted-carson-2pepk9` backend, `claude/trusting-cori-2pepk9` frontend). Repo CLAUDE.md also forbids pushing to main.

## Missing tests / verification
7. Backend gate last run: see CHANGELOG 2026-10-09. IMAP IDLE unverified against a real server (CLAUDE.md roadmap).
8. Flutter/Dart not installed in the routine environment — frontend gates cannot run until it is.

## Housekeeping
9. `master/` dir (PLAN/BACKLOG/CHANGELOG from an earlier routine) coexists with root `CHANGELOG.md`; consolidate.
