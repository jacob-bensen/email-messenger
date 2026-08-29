# Internal TODO — email-messenger

Running list of gaps discovered by the full-stack sync routine (the
one that keeps this backend and `../conexus-mail-mobile` in sync via
`docs/API.md`). Distinct from `master/BACKLOG.md` and
`master/PLAN.md`, which are the pre-existing operator routine's
backlog and belong to a different workflow — do NOT fold the two
lists together.

Severity: contract breakage > logic drift > missing tests > style.

## Contract breakage

- **`/api/mobile/v1/**` does not exist.** The mobile client's entire
  API surface is planned-only in `docs/API.md § 2`. First feature run
  should implement `POST /api/mobile/v1/auth/login`,
  `POST /auth/logout`, `GET /me` (M1.1 in
  `../conexus-mail-mobile/docs/DESIGN.md`). Requires a
  `mobile_session` table (new Flyway migration V36) and a small
  `MobileAuthService` that reuses `AuthService` for credential
  verification. Do NOT invent a parallel auth path — reuse the
  existing password check and `AuthEvent` writes.

## Logic drift

- None right now, because there's no client to drift against. On
  every future run, before adding a mobile endpoint, verify that the
  underlying rule already lives in a `service/` class — if it lives
  in a web controller instead, refactor first, then expose.

## Missing tests

- No slice test yet asserts that JSON endpoints return the same
  domain-level result as the analogous Thymeleaf endpoint (e.g.
  `POST /api/mobile/v1/chats/{key}/reply` vs.
  `POST /chats/{key}/send`). Add a shared service-level test that
  both controllers delegate to, when the mobile endpoints land.

## Style / infrastructure

- `docs/BACKEND.md`, `docs/API.md`, `docs/WEB.md` are new (2026-08-29).
  Keep them updated on every controller change.
- The Stripe webhook is the only non-mobile JSON endpoint and lives in
  `billing/`, not `web/`. This is fine; the API doc calls it out
  explicitly so nobody adds a mobile controller alongside it.
- Consider adding a repo-root `CONTRIBUTING.md` link that points at
  both `CLAUDE.md` and `docs/BACKEND.md` for new contributors — not
  urgent while the team is one person.
