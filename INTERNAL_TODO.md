# INTERNAL_TODO (backend)

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
- S1. **No mobile API exists.** The mobile client has nothing to call. Decide
  token auth (session cookie + CSRF is awkward for Flutter) and add
  `/api/v1/**` per docs/API.md, reusing `ConversationListService`, `ChatService`,
  `ReplyService`, `MailAccountService`. Needs its own SecurityFilterChain
  (stateless, bearer token, CSRF off for `/api/**`).
- S2. No JSON error schema; all errors are HTML via `GlobalExceptionHandler`/`/error`.

## Logic drift (web vs service layer)
- L1. `ConversationController` injects `EmailThreadRepository` directly instead of
  going through a service; audit other controllers for repository access before
  an API reuses them.
- L2. `ConversationController.list` triggers `refreshDueForUserAsync` as a side
  effect of a GET; an API list endpoint should not copy that implicitly.

## Missing tests / verification
- T1. IMAP IDLE not verified against a real server (see CLAUDE.md roadmap).

## Housekeeping
- H1. `master/` directory (PLAN/BACKLOG/MASTER_ACTIONS/CHANGELOG from earlier
  routines) is tracked on the branch; root CHANGELOG.md now exists for this
  routine. Consider merging/moving `master/CHANGELOG.md`.
- H2. Branching: the routine prompt says commit to `master`; the session harness
  requires `claude/gifted-carson-it03x5`. Branch rule from the harness was
  followed. Reconcile the prompt.
- H3. Stack differs from routine's assumption (JPA, not jOOQ).
