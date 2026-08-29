# INTERNAL_TODO — email-messenger

Running list of gaps, contract mismatches, and deferred work. Consumed
from and appended to on every run. Ordered by severity within each
section.

## Contract mismatches (backend ↔ mobile)

The mobile client (`jacob-bensen/conexus-mail-mobile`) is empty and no
JSON REST API exists on the server. No feature can be built end-to-end
across both repos until this is unblocked.

- **BLOCKER-1 (needs user decision).** Do we actually want a mobile
  client for ConexusMail, and if so, does it use Flutter as the mobile
  repo README claims? Confirm before scaffolding.
- **BLOCKER-2 (needs user decision).** Auth model for the mobile
  client: bearer token issued by the server (recommended — reuses
  `UserService`), OAuth2 password / device-code, or something else?
  Answer drives the first API endpoints.
- **API-1.** No JSON endpoints exist. `docs/API.md` sketches the
  contract format to adopt; every `/api/v1/*` endpoint will need
  matching Dart DTOs in the mobile repo.
- **API-2.** No error envelope has been chosen. Pick `{code, message,
  details?}` (or equivalent) before writing the first endpoint so all
  clients can share deserialization.
- **API-3.** No pagination scheme. `/chats` today is server-rendered
  and unbounded; a mobile API for the same view needs cursor pagination
  keyed on `email_threads.updated_at`.

## Backend gaps (independent of mobile)

Roadmap items already tracked in `CLAUDE.md`; the highest-value
unblocked one is called out here for quick reference.

- **Live delivery, part 2 — Web Push** (next unchecked non-blocked
  roadmap item). VAPID keys, `push_subscriptions` table, opt-in on
  `/account/notifications`, service-worker `push` handler, per-conv
  preview payload.
- **Delivery states for outbound bubbles.** Sending/Sent/Delivered/Not
  Delivered, DSN parsing, MAILER-DAEMON suppression on the chats list.
- **Signature → contact card**, **per-person shared files**,
  **reply-to-a-specific-bubble**. See root `CLAUDE.md` roadmap.
- **Attachments are not downloadable.** `attachments.blob_ref` is
  always null; no blob store, no download endpoint. Blocking any
  meaningful attachments UX (mobile included).
- **IMAP IDLE not verified** against a real server per the roadmap
  note — worth confirming next time the app runs locally with a
  connected mailbox.

## Web ↔ API duplication risk

Currently harmless because there is no API, but every new controller
must keep its logic in the service layer so the future JSON endpoint
can call the same method. Enforce this in review.

## Tests

- Backend: `./mvnw test` — Testcontainers Postgres + GreenMail. Run
  this before every commit; the routine calls for it.
- Mobile: no test framework yet (repo is empty).
- No CI. Per the roadmap, do not re-add GitHub Actions — tests run
  locally.

## Deferred / notes

- Support email address decision (roadmap item, blocked on user).
- Publish Google OAuth consent screen (roadmap item, blocked on
  user — external Google review process).
- Docker files (`Dockerfile`, `docker-compose.yml`, `DEPLOY.md`) still
  present at repo root even though CLAUDE.md notes Docker was retired
  2026-06-11. Consider deleting on a future cleanup pass; leaving for
  now to avoid a scope-creep commit inside a doc-bootstrap run.
