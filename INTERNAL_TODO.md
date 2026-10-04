# INTERNAL_TODO

Ordered by severity: contract breakage > logic drift > missing tests > style.
(Sync audit performed 2026-10-04; client repo is an empty scaffold.)

## Contract breakage
- [ ] **No mobile API exists.** `docs/API.md` §B is a plan only. Needed first:
      `/api/v1` JSON controllers sharing `ChatService`/`ConversationListService`.
- [ ] **No token auth.** Only cookie session + CSRF (`SecurityConfig`). Design
      Bearer/refresh tokens (+ Google sign-in for mobile) in a new
      `SecurityFilterChain` scoped to `/api/**` (stateless, CSRF off there).
- [ ] No JSON error envelope; `GlobalExceptionHandler` returns HTML views only.
- [ ] `GET /chats/events` SSE payload is undocumented (read `NewMailEvent`).

## Logic drift / structure
- [ ] Mailbox scoping (`selectMailbox`, `scopeAddress`, `sendingAccount`) and
      send-vs-reply branching are private methods in `ConversationController`;
      extract to a service so web + API share them.
- [ ] `ChatService`/`ConversationListService` are package-private in `web`;
      widen or relocate when the API controller lands.
- [ ] Task spec assumes jOOQ; repo uses JPA. Treat JPA as the convention.

## Missing / blocked
- [ ] Attachments have no bytes (`blob_ref` null) — mobile can't download them.
- [ ] `ReplyService` threads onto the thread's last message (see CLAUDE.md roadmap).
- [x] Baseline `mvn -q -B test` passed (exit 0) on 2026-10-04.
- [ ] Repo-level roadmap in CLAUDE.md has open items (support email, Google
      OAuth consent publish, Web Push, delivery states…); `master/` dir holds
      older planning notes (PLAN/BACKLOG/MASTER_ACTIONS, 500KB CHANGELOG).
- [ ] Branch policy: routine says commit to `master`; this session's
      harness mandated feature branches. Pushed to the branches instead.
