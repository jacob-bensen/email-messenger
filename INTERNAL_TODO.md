# INTERNAL_TODO (ordered by severity)

## Contract breakage
- [ ] No mobile REST API exists; `docs/API.md` §2 is only a plan. Mobile repo has no code. (Both sides: nothing to mismatch yet.)
- [ ] No token auth (session cookie + CSRF only). Needs `/api/v1` security chain with Bearer tokens before mobile can log in.

## Logic drift
- [ ] Routine assumes jOOQ; backend uses JPA. Docs/routine should be corrected; no jOOQ codegen to run.
- [ ] Logic lives partly in controllers (`ConversationController` builds list model, filter parsing, mailbox selection). Extract to services before an API reuses it.
- [ ] Attachments not downloadable (`blob_ref` null) — blocks mobile file viewing.

## Missing tests / tooling
- [ ] Gate results unverified this run (docs-only; `./mvnw test` not run).

## Deferred (from CLAUDE.md roadmap)
- Web Push, delivery states, reply-to-bubble, signature contact card, support email, Google OAuth publish.

## Process conflict
- Routine says work on `master`; this session's harness mandates branches `claude/gifted-carson-7q35e4` (backend) and `claude/trusting-cori-7q35e4` (mobile). Branch rules followed; merge to master is manual.
