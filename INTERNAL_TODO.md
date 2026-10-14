# INTERNAL_TODO (ordered by severity)

## Contract breakage
- No `/api/**` REST API and no token auth exist; the mobile client (`conexus-mail-mobile`) is an
  empty repo. Define `docs/API.md` slice 1 (auth + chat list/detail/send) before building either side.

## Logic drift
- None detectable yet (single code path: web controllers → services). Re-audit once an API exists.

## Environment / tooling
- Routine prompt assumes jOOQ; backend uses JPA. Routine prompt says commit to `master`; this
  session was constrained to branch `claude/gifted-carson-5ejjko` instead.
- `flutter`/`dart` not installed in the cloud environment; frontend gates cannot run there.
- Backend has pre-existing `master/` dir (PLAN/BACKLOG/CHANGELOG for the operator routine) — distinct from
  root `CHANGELOG.md`.

## Missing tests / style
- Not audited this run.
