# Changelog — full-stack sync routine

Entries the two-repo sync routine adds; each corresponds to a matching
entry in `conexus-mail-mobile/CHANGELOG.md`. Product-level history for
the SaaS lives separately in `master/CHANGELOG.md` (owned by the
single-repo product routine).

## 2026-08-29 — Phase 0 bootstrap + first API contract

- Repo classification: `email-messenger` = BACKEND, `conexus-mail-mobile`
  = FRONTEND.
- Bootstrapped doc system: `docs/BACKEND.md`, `docs/API.md`,
  `docs/WEB.md`, root `CHANGELOG.md`, `INTERNAL_TODO.md`.
- Added the first REST endpoint the mobile client can consume:
  `GET /api/v1/plans` (public, read-only pricing catalog). Sources every
  figure from the same `billing.PlanPricing` + `billing.PlanLimits`
  helpers the admin MRR dashboard reads, so `/pricing` HTML, the
  operator dashboard, and the mobile app cannot drift.
- Refactor: promoted `PlanPricing` from
  `com.emailmessenger.admin` (package-private) to
  `com.emailmessenger.billing` (public) with matching import updates in
  the 5 admin services that used it. No behavior change.
- Security: whitelisted `/api/v1/plans` as an unauthenticated route.
- Tests: 6 new `PlansApiControllerTest` cases, all green.
