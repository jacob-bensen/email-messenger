# INTERNAL_TODO.md — backend

Running list of known gaps, tech debt, and deferred work. Ordered by
severity (contract breakage > logic drift > missing tests > style).
Consumed and appended to on every run of the full-stack sync routine.

## Contract mismatches (backend ↔ mobile client)

- **Zero mobile REST API exists in code.** `docs/API.md` §Endpoints
  under `/api/v1/**` are all 🚧 Planned. The Thymeleaf-first server has
  only two JSON producers today: `POST /chats/refresh` and
  `POST /billing/webhook` (Stripe). The mobile client has nothing to
  call. First real feature end-to-end must be `GET /api/v1/me` +
  `POST /api/v1/auth/login` because everything else depends on
  bearer-token auth.
- **No auth scheme for `/api/**` yet.** Website uses session cookies
  and Spring Security's form + OIDC filters; mobile bearer tokens are
  not implemented. Need a `SecurityConfig` that pattern-matches
  `/api/**` and installs a bearer filter, plus a
  `MobileSessionToken` entity (Flyway V36) and a
  `MobileSessionService`.
- **No `/api/**` CSRF exemption.** When bearer auth lands, add
  `.csrf(c -> c.ignoringRequestMatchers("/api/**"))` because the
  mobile client can't produce a CSRF token.

## Logic drift risks

- The `/chats` web workspace calls `MailboxPollingService.refreshDueForUserAsync`
  on every navigation. The mobile API surface must NOT duplicate that
  side-effect inside a controller — it belongs in the service and
  should be reused by the mobile handler.
- `PlanLimitEnforcer` is invoked from several web controllers ad-hoc.
  When the mobile send endpoint lands, it MUST hit the same
  enforcer; verify by extracting a helper method rather than
  re-checking limits in a REST controller.

## Missing tests

- No slice test yet covers the JSON response shape of
  `POST /chats/refresh`. When the mobile parity endpoint
  (`POST /api/v1/mailbox/sync`) lands, add a shared
  `@WebMvcTest` verifying both endpoints return the same field.

## Docs debt

- `docs/BACKEND.md` §Auth model says "Planned: bearer tokens..." —
  update this in the same PR that lands the tokens.
- `docs/API.md` currently marks every `/api/v1/**` endpoint 🚧. Flip
  entries to ✅ as they ship. Every ✅ endpoint must have a slice
  test asserting its exact response shape.
- The public README (`README.md`) does not yet mention the mobile
  client. Add a "Related projects" section pointing at
  `conexus-mail-mobile` once at least one mobile-serving endpoint is
  ✅.

## Style / hygiene

- Two `@RequestMapping` classes (`SavedSearchController`,
  `MailboxController`, `GmailConnectController`) use base paths.
  Keep the same convention when adding `/api/v1/**` controllers:
  put the version + area on the class and only the tail on the
  method.

## Cross-repo sync backlog

See also `/home/user/conexus-mail-mobile/INTERNAL_TODO.md`. Both files
should be kept aligned each run; a mismatch between them is itself a
sync bug.
