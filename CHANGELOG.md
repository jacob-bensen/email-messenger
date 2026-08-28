# Changelog

Dated entries from the full-stack sync routine. Newest on top.

## 2026-08-28 — Phase 0 bootstrap (backend)

- Created `docs/BACKEND.md`, `docs/API.md`, `docs/WEB.md` describing the
  current server architecture and the REST contract the mobile client
  in `../conexus-mail-mobile` will consume.
- Created this `CHANGELOG.md` and `INTERNAL_TODO.md`.
- Added a "Related repositories" section to `CLAUDE.md` naming the
  mobile sibling repo.
- Recorded the two headline sync gaps: the REST API for the mobile
  client does not exist (backend is Thymeleaf-only), and the mobile
  repo is empty (no Flutter scaffold yet). Both are top of
  `INTERNAL_TODO.md`.
- No production code changed this run; no migration added; no test
  status change. Followed the routine's guidance that a first run may
  be entirely discovery + docs.
- Branch policy: the scheduled prompt asks for pushes to `master`, but
  the harness policy for this session pins pushes to
  `claude/relaxed-newton-42qz3g` and explicitly forbids pushing to a
  different branch without explicit user approval. This run follows the
  harness policy.
