# INTERNAL_TODO (ordered by severity)

## Contract breakage
- No JSON/REST API exists; `conexus-mail-mobile` has nothing to call. Needs token auth + `/api/v1/*`
  designed in docs/API.md first. (Product decision needed: mobile is a native client vs. the PWA.)

## Logic drift
- Routine assumes jOOQ + `lib/domain` mirror; backend uses JPA, mobile has no code yet. Update the routine
  prompt or confirm direction.
- `master/` holds an older planning/CHANGELOG system (500KB); root CHANGELOG.md starts 2026-10-09.

## Missing tests / unverified
- Backend `./mvnw verify` not run during the Phase 0 run (docs only).
- IMAP IDLE unverified against a real server (see CLAUDE.md roadmap).

## Process
- Routine says work on `master`; session harness pins `claude/gifted-carson-73tgq3` /
  `claude/trusting-cori-73tgq3`, and CLAUDE.md says never push to main. Pushed to the harness branches.
