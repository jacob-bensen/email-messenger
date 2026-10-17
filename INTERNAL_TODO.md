# Internal TODO (sync audit, 2026-10-17)

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
1. **No REST API exists for the mobile client.** `docs/API.md` §3 is a sketch only. The sibling repo
   `conexus-mail-mobile` is an empty Flutter placeholder (README only), so nothing is *currently*
   broken, but no feature can be built "end-to-end" until the items below land.
2. **No token auth.** Only session cookie + CSRF. Need a bearer/refresh-token scheme (new Flyway
   migration, `SecurityFilterChain` for `/api/**` with CSRF disabled), decision on Google sign-in on mobile.
3. `GET /chats/events` SSE payload is undocumented (contract TBD).
4. No JSON error envelope (`GlobalExceptionHandler` is HTML only).

## Logic drift (web vs. future API)
5. `ConversationController.send` / `/chats/new` hold business logic (recipient list from members,
   signature via `withSignature`, sending-account choice, reply-vs-new, `OutboundMessageService.record*`).
   Extract into a service (e.g. `ChatSendService`) so web and API share it.
6. `ConversationController.populateList` / `selectMailbox` / `scopeAddress` mailbox-scoping rules
   live in the controller; move to a service before exposing via API.
7. `ReplyService` always threads onto the thread's last message (also on CLAUDE.md roadmap).

## Missing tests / tooling
8. Backend: `mvn -B test` passes online (794 run, 0 fail, 1 skipped); offline build impossible (Spring Boot parent not cached).
9. Frontend: no project. `flutter`/`dart` are not installed in the routine environment.

## Process
10. Routine prompt says commit to `master`; the cloud session pins feature branches
    (`claude/gifted-carson-pyvy1n`, `claude/trusting-cori-pyvy1n`). Work was done on those branches.
    Routine prompt also assumes jOOQ; backend uses JPA.
