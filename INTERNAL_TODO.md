# INTERNAL_TODO

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
- [ ] **No mobile API exists.** `docs/API.md` §3 is all TBD; the mobile repo has no code. First feature run should pick the auth scheme (bearer tokens vs cookie) and ship a read-only slice (`/api/v1/me`, conversations list/detail) on top of `ChatService` / `ConversationListService`, with the client DTOs in the same run.
- [ ] Security: `/api/**` needs bearer auth and CSRF exemption added to `SecurityConfig` (currently CSRF on for all non-webhook routes).

## Logic drift
- [ ] `master/PLAN.md` (stale) describes Free/Personal/Team/Enterprise pricing and "MailIM"; code has Free/Pro/Business (`PlanLimits`: Free 3 mailboxes + 500 threads). Reconcile or retire PLAN.md.
- [ ] Reply-to-bubble mis-threads onto the thread's last message (`ReplyService`) — roadmap item in CLAUDE.md.

## Missing tests / verification
- [ ] IMAP IDLE never verified against a real server (roadmap note).
- [ ] Baseline `./mvnw test` result not recorded this run (docs-only run).

## Deferred / process
- [ ] Routine prompt assumes jOOQ and `master`-only commits; repo uses JPA and this session was pinned to feature branches. Update the scheduled prompt or branch policy.
- [ ] Two changelogs: historical `master/CHANGELOG.md` (~500 KB) vs new root `CHANGELOG.md`. Decide which is canonical.
- [ ] `attachments.blob_ref` always null (no downloads) — blocks mobile attachment viewing.
- [ ] Support email + Google OAuth consent-screen publish (CLAUDE.md roadmap) still open.
