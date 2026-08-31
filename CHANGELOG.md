# Changelog — email-messenger

Dated entries appended by the full-stack sync routine. One entry per
run; keep the "what and why", not a code dump.

## 2026-08-31 — Phase 0 bootstrap

- Reverse-engineered the current backend into `docs/BACKEND.md`
  (architecture, package layout, auth model, services).
- Reverse-engineered the Thymeleaf website into `docs/WEB.md` (route
  map, fragments, service dependencies).
- Established `docs/API.md` as the shared contract with
  `conexus-mail-mobile`. Only `GET /api/v1/status` is defined so
  far; the rest is listed as planned so both repos can plan against
  it without coding to a promise.
- Added `com.emailmessenger.api` package with `StatusApiController`
  and a slice test. The controller is public in `SecurityConfig` (no
  session required), returns a `StatusResponse` record, and gives
  the mobile app something concrete to point at before the real API
  surface lands.
- Seeded `INTERNAL_TODO.md` with the contract mismatches and open
  decisions the audit surfaced (mobile auth mechanism, live-delivery
  transport for mobile, attachment blob store).
- Added a "Related repositories" pointer to the top of `CLAUDE.md`
  so both single-repo and full-stack sessions know where the
  frontend lives.
