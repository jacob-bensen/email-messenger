# CHANGELOG

(Historic per-iteration log lives in `master/CHANGELOG.md`.)

## 2026-10-04 — Full-stack sync routine, Phase 0 bootstrap
- Added `docs/BACKEND.md`, `docs/API.md`, `docs/WEB.md`, `INTERNAL_TODO.md`
  by reverse-engineering the code; no behavior changed.
- Finding: no REST/token API exists for the mobile client; recorded in
  INTERNAL_TODO and as planned contract v1 in `docs/API.md` §B.
- CLAUDE.md: added "Related repositories".
- Baseline `mvn -q -B test`: exit 0 (all tests pass). `verify` not run separately.
