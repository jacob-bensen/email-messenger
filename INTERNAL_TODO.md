# INTERNAL_TODO

Running list of known gaps, tech debt, and deferred work discovered by
the full-stack sync routine. Consume from the top and append new items
at the bottom of the matching section. Severity: contract-breakage >
logic drift > missing tests > style.

## Contract / sync (blocks mobile parity)

- **Bearer-token auth for `/api/**`.** Today the API accepts the session
  cookie and CSRF is off for `/api/**`; only GET endpoints are exposed.
  Before shipping any mutating endpoint (reply, mark-read, connect
  mailbox), add a token flow — likely OAuth2 password grant against the
  existing `DbUserDetailsService`, or a personal-access-token table
  scoped per device. Update `docs/API.md` §Authentication when this
  lands.
- **`GET /api/v1/chats/{key}` timeline endpoint** — mobile client will
  need this immediately after the chat list. Shape drafted in
  `docs/API.md` under "Planned endpoints" but not implemented.
- **`GET /api/v1/me`** — mobile client needs profile / plan / quota to
  render its account tab.

## Tech debt (backend)

- Routine template names the target stack "jOOQ + PostgreSQL", but this
  repo uses Spring Data JPA. Not worth migrating; the divergence is
  noted in `docs/BACKEND.md` so future routine runs don't try to switch.
- `master/` directory at the repo root contains PLAN/BACKLOG/CHANGELOG
  files from an earlier autonomous-routine attempt. Not referenced by
  the current CLAUDE.md roadmap; decide whether to fold its EPIC-18
  status ("Send win-back CTA shipped, conversion card open") into the
  main CLAUDE.md roadmap or delete the directory.

## Tests / gates

- Flutter/Dart tooling isn't installed in the current session
  environment, so `dart format`, `flutter analyze`, and `flutter test`
  cannot run. Backend gate `./mvnw test` runs.

## Style

- (none yet)
