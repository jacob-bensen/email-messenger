# Internal TODO

Known gaps and deferred work, most severe first. Mobile-side items live in
`conexus-mail-mobile/INTERNAL_TODO.md`; roadmap features live in
`CLAUDE.md` (website) and the mobile `docs/DESIGN.md` §ROADMAP (API).

## 1. Contract breakage
- (none known — `docs/API.md` §5 matches `api.*` and the mobile `lib/data/`
  as of 2026-11-15)

## 2. Logic drift (web vs API, server vs mirror)
- [ ] **Send flow lives in `ConversationController`**, not a service:
      recipient parsing, sending-account choice, signature append, subject
      defaulting, reply-vs-new routing and outbound recording
      (`/chats/new`, `/chats/{key}/send`). Extract into a service before
      the API exposes reply/new-message (mobile M2).
- [ ] `ConversationController` reads `EmailThreadRepository` directly for
      conversation lookups; list/chat assembly should be reachable through
      `ConversationListService` / `ChatService` alone before the
      conversation-list API (mobile M1) reuses it.
- [ ] `ConversationLabels.initials` uppercases with the default JVM locale
      (`toUpperCase()`); the Dart mirror is locale-independent. Use
      `Locale.ROOT` (and add a shared vector) when next touching it.
- [ ] `GlobalExceptionHandler.planLimit` redirects to `/threads` (itself a
      redirect to `/chats`); the API will need a JSON `plan_limit_exceeded`
      error instead when mailbox/thread creation is exposed.

## 3. Missing tests / gaps
- [ ] Unknown `/api/**` paths fall through to the website's HTML 404
      (`NoResourceFoundException` is handled by `GlobalExceptionHandler`,
      not `ApiExceptionHandler`). Return the JSON envelope instead.
- [ ] No API rate limit beyond the login throttle.
- [ ] No device list / "sign out everywhere" for `api_tokens`; expired rows
      are never purged.
- [ ] Google-only accounts cannot sign in through the API (mobile M3).
- [ ] 1 test is skipped in `mvn verify` (Testcontainers/Docker-dependent);
      run it where Docker is available.

## 4. Style / hygiene
- [ ] `master/` holds old planning docs (`PLAN.md`, `BACKLOG.md`, …) using
      a previous product name ("MailIM") and pricing; reconcile or remove.
