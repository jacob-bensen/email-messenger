# Internal TODO

Known gaps and tech debt from the 2026-11-16 sync audit, most severe first.
Client-side items live in `conexus-mail-mobile/INTERNAL_TODO.md`. Product
roadmap: `CLAUDE.md` §Roadmap (website) and the mobile repo's
`docs/DESIGN.md` §ROADMAP (cross-repo).

## 1. Contract breakage
- [ ] **No mobile API at all** (`docs/API.md` §1). The mobile client cannot
      sign in or read anything. Next cross-repo item: `/api/v1` stateless
      bearer chain + `api_tokens` migration + `POST /auth/login`,
      `POST /auth/logout`, `GET /me` + JSON error envelope (§2, §3.1, §5).

## 2. Logic drift (web vs. future API)
- [ ] Compose/reply rules live in `ConversationController` (recipient
      parsing/validation, empty-body check, "(no subject)" default,
      signature append, sending-account choice, reply-vs-new decision).
      Extract into a service before `API.md` §3.3 is built.
- [ ] `MailboxController`: sync-frequency floor validation, 2000-char
      signature cap, sync ownership check — move into `MailAccountService` /
      `MailboxPollingService` before the mailbox API (§3.4).
- [ ] `PasswordResetController` owns the min-password-length (8) rule;
      `AccountService.changePassword` / `UserService.register` should share
      one constant/validator.
- [ ] `DigestController` and `TeamInviteController` hit repositories
      directly.
- [ ] `ConversationLabels.initials` uses `toUpperCase()` with the default
      locale (Dart mirror is locale-independent). Use `Locale.ROOT`.

## 3. Bugs / suspicious
- [ ] `GlobalExceptionHandler` turns `PlanLimitExceededException` into a
      flash attribute + redirect to `/threads`, which redirects again to
      `/chats`; flash attributes survive one redirect, so the upgrade modal
      may never render. Unverified — write a MockMvc test.
- [ ] Redirects/comments still target `/threads` (e.g. landing, register,
      mailbox connect) though it only bounces to `/chats`.
- [ ] Migration comments contradict code (V26 mentions PERSONAL/TEAM/
      ENTERPRISE; V10 says Free = 1 saved search). Code is right; don't edit
      applied migrations — just don't trust those comments.

## 4. Missing tests
- [ ] No `@WebMvcTest` slices; controller behavior is covered only by
      `@SpringBootTest`.
- [ ] `EndToEndMailboxFlowIntegrationTest` is skipped without Docker (always
      skipped in cloud sessions).
- [ ] IMAP IDLE not verified against a real server (see CLAUDE.md roadmap).

## 5. Hygiene
- [ ] `master/` planning docs are stale (2026-06, old plan names). Decide
      whether to delete or archive; ops TODOs in `master/MASTER_ACTIONS.md`
      are still relevant.
- [ ] `CLAUDE.md` "Tech stack (target)" predates the current code (Spring
      Security, Stripe, OAuth not listed).
