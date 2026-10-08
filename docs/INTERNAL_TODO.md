# INTERNAL_TODO.md

## Sync audit 2026-10-08 (ordered by severity)
### Contract breakage
- [ ] **No REST API exists** for the mobile client; `docs/API.md` "Planned" is a sketch only. Need `/api/v1` + token auth (stateless chain for `/api/**`, CSRF-exempt) before any client networking.
- [ ] **Frontend repo `conexus-mail-mobile` is empty** (README only; no pubspec, no `lib/`). Needs `flutter create`, `lib/domain/` skeleton, networking layer.
### Logic drift
- [ ] Persistence is JPA, not jOOQ as the routine's template assumes; docs/CLAUDE.md reflect JPA. No action unless jOOQ is wanted.
- [ ] Controllers `ConversationController` (~400 lines) hold some orchestration (mailbox scoping, `successMessage` flows); extract into a service before the API reuses it.
- [ ] Roadmap item "Reply-to-a-specific-bubble": `ReplyService` always threads onto last message — API must not bake that in.
### Missing tests
- [ ] SSE `/chats/events` payload shape is undocumented and untested at contract level.
- [ ] No full `mvn verify` result recorded this run (see CHANGELOG).
### Style / process
- [ ] Routine says work on `master`; this session was pinned to `claude/gifted-carson-4bvndk` (backend) / `claude/trusting-cori-4bvndk` (mobile) and CLAUDE.md says don't push to main. Reconcile the routine prompt with the branch policy.
- [ ] Existing roadmap in `CLAUDE.md` (support email, Google OAuth publish, Web Push, delivery states, ...) remains the web product backlog.
