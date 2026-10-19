# INTERNAL_TODO

Ordered by severity (contract breakage > logic drift > missing tests > style).

## Contract breakage
- [ ] No mobile API exists. `docs/API.md` §2 is a proposal only. Needs: token auth chain for `/api/**` (CSRF-exempt, stateless), `/api/v1` controllers over the existing services, API error format. Decide first.
- [ ] Mobile client has no code (README only): no `pubspec.yaml`, `lib/`, or platform folders.
- [ ] Attachments are not downloadable (`blob_ref` always null) — blocks any attachment UI on mobile.

## Logic drift
- [ ] Controllers (`ConversationController`) hold nontrivial orchestration (sender selection, signature appending, recipient parsing, key computation, send + record). Extract into a service before an API reuses it, so web and API share one path.
- [ ] Sync routine assumes jOOQ; backend uses JPA. Routine's codegen step is N/A.
- [ ] Routine says commit to `master`; this session's branch instructions say `claude/gifted-carson-gpeoux` (backend) / `claude/trusting-cori-gpeoux` (mobile). Branch instructions were followed; reconcile.

## Missing tests / verification
- [ ] `./mvnw test` not run in this bootstrap run (docs-only change); run next code-changing run.
- [ ] IMAP IDLE live delivery still unverified against a real server (see CLAUDE.md roadmap).

## Style
- [ ] Root CLAUDE.md "Target package layout" is stale vs. actual packages (see `docs/BACKEND.md`).
