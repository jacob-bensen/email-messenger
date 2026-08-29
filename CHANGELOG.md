# CHANGELOG

Per-run notes from the shared full-stack sync routine that spans this repo
and `conexus-mail-mobile`. The long-standing per-feature changelog kept by
the roadmap routine still lives at `master/CHANGELOG.md` — do not merge the
two; this file is for the cross-repo sync work.

## 2026-08-29

Phase 0 bootstrap. First run of the full-stack sync routine against this
repo and the sibling `conexus-mail-mobile` client.

- Classified repos: this is the BACKEND (Java 21 + Spring Boot 3 +
  Thymeleaf + Spring Data JPA + Flyway + Maven; PostgreSQL 16). Note the
  routine's generic stack template names jOOQ — this project uses JPA.
- Confirmed the sibling `conexus-mail-mobile` repo is a Flutter skeleton
  with only a README committed; no `pubspec.yaml`, no `lib/`.
- Confirmed the backend exposes no REST API for a mobile client today
  (single `@RestController` is the Stripe webhook); every user-facing
  route renders Thymeleaf HTML.
- Created `docs/BACKEND.md` — module map, stack, data model, auth model.
- Created `docs/API.md` — canonical mobile-facing REST contract, all
  endpoints marked TBD since none exist yet.
- Created `docs/WEB.md` — full inventory of Thymeleaf routes so REST
  endpoints, when they land, can reuse the same services rather than
  fork logic.
- Added `INTERNAL_TODO.md` — sync-audit gaps and prerequisites for the
  mobile client (top item: bearer-token auth so the Flutter app can
  authenticate; today's cookie-session model does not work for it).
- Updated `CLAUDE.md` with a "Related repositories" section pointing at
  the mobile repo and naming each doc.

Not done this run: no code changes; test suite not run (nothing changed
that could affect it).
