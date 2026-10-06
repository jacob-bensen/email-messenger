# INTERNAL_TODO

Ordered by severity (contract breakage > logic drift > missing tests > style).
Audit date: 2026-10-06 (first sync-routine run).

## Contract breakage
- [ ] No REST/JSON API exists; the mobile repo has nothing to call. Build `/api/v1`
      per `docs/API.md` (auth first, then chats list/detail/send), as thin
      controllers over `ChatService`, `ConversationListService`, `OutboundMessageService`.
- [ ] No token auth: security is session + CSRF only. Add a stateless bearer filter chain
      for `/api/v1/**` (CSRF off there) with refresh tokens.
- [ ] Attachments have no download endpoint (`blob_ref` null) — mobile needs it.

## Logic drift
- [ ] Routine template assumes jOOQ; repo uses JPA. Keep JPA; routine prompt should be adjusted.
- [ ] Verify `ConversationController` doesn't hold business logic that a REST controller
      would have to duplicate (extract into `ChatService` when building the API).

## Missing tests
- [ ] None audited yet for the API (nothing exists).

## Process
- [ ] Routine says commit to `master`; this session's harness mandates branch
      `claude/gifted-carson-6gre5m` (backend) / `claude/trusting-cori-6gre5m` (mobile) and
      CLAUDE.md forbids pushing to main. Work was pushed to those branches.
- [ ] Mobile repo is empty (README only, no pubspec). Scaffold with `flutter create`
      (flutter/dart not installed in this environment).
