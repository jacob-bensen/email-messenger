# Internal TODO — email-messenger

Running list of gaps, tech debt, and deferred work. Autonomous runs
consume from and append to this file. Ordered severity-first: contract
breakage > logic drift > missing tests > style.

The active product roadmap lives in `CLAUDE.md` and is the primary
source for "what to build next" — this file is for cross-repo drift and
tracked debt that doesn't fit a roadmap checkbox.

## Contract drift (cross-repo)

- **No mobile JSON API exists.** `docs/API.md` lists a planned surface;
  none of it is implemented. The sibling `conexus-mail-mobile` client
  cannot make progress until at least the auth + chats + reply
  endpoints ship. Every autonomous run that wants to advance the
  mobile side must first land those endpoints here (contract-first).
- **SSE stream is cookie-authed only.** `GET /chats/events` requires
  the Thymeleaf session cookie. Once a JSON auth endpoint issues the
  same cookie for the mobile client, the stream is reachable as-is
  — otherwise a token-friendly variant will be needed.

## Tech debt

- `master/` directory at the repo root holds legacy planning docs
  (`BACKLOG.md`, `CHANGELOG.md`, `MASTER_ACTIONS.md`, `PLAN.md`) from
  an earlier autonomous routine. Its content should be triaged (fold
  useful items into `CLAUDE.md` roadmap or this file, then delete)
  once someone has time — it is currently orphaned but harmless.
- The scheduled-routine prompt template mentions jOOQ; this project
  uses Spring Data JPA. Documented in `docs/BACKEND.md`. No action
  needed unless a jOOQ migration is actually planned.

## Missing tests

- No slice test yet for the SSE `GET /chats/events` route. Add one
  once we have a repeatable way to assert an `NewMailEvent` fan-out.

## Style / small items

None tracked yet.

## Follow-ups discovered during this run

- The two roadmap items in `CLAUDE.md` currently blocked on external
  decisions (support-email address, Google OAuth consent-screen
  publish) are user-owned; do not attempt to "fix" them by picking a
  placeholder address or by opening a Google Cloud console tab.
- `README.md`'s screenshot section is still a placeholder; not a
  functional gap but worth updating when the chat UI is stable.
