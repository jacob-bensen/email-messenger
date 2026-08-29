# Internal TODO — email-messenger

Running list of gaps and deferred work discovered by the sync routine.
Feature-shaped work lives on the roadmap in `CLAUDE.md` — this file is
for cross-cutting mismatches, tech debt, and questions the routine can
raise but cannot decide on its own. Ordered by severity within each
section.

## Product decisions the routine cannot make

- **Do we want a mobile client?** The sibling `conexus-mail-mobile`
  repo is empty (README + .gitignore) — no product design, no
  scaffolding, no API contract. Every mobile item below is blocked on
  this decision. If yes: what platform (Flutter as the repo name
  implies, or something else), which flows ship first, and does it
  reuse the browser session cookie via WebView or need a real JSON
  API + token auth?
- **Support email address.** First unchecked roadmap item in
  `CLAUDE.md`. Needs a real address and an outbound sender decision.
- **Google OAuth consent screen — publish to production.** Second
  unchecked roadmap item. Owner action outside the code.

## Contract gaps (would matter the moment a mobile client exists)

- No non-cookie auth path (bearer / OIDC for a native app).
- No JSON representation of a conversation, bubble run, or message.
  The `/chats/{key}/pane` fragment endpoint is HTML.
- `NewMailEvent` on `/chats/events` is an SSE hint tuned for the
  browser morph client; there is no JSON schema for it.
- Attachments are not downloadable — `Attachment.blob_ref` is always
  null (see roadmap "Per-person shared files & links"). A mobile
  client would hit this immediately.
- Web Push is unimplemented (roadmap "Live delivery, part 2"). A
  mobile client would want a device-push contract at the same time.

## Verification debt

- IMAP IDLE (`ImapIdleService`) has not been verified against a real
  IDLE-supporting server — noted in the roadmap. Do this the next
  time the app is run locally against a real mailbox.

## Documentation drift to watch

- `CLAUDE.md` "Target package layout" is a subset of the actual
  layout — the actual layout is in `docs/BACKEND.md`. Keep both
  honest; if they diverge again, treat `docs/BACKEND.md` as truth
  and update `CLAUDE.md`.
- `docs/API.md` should grow a real "Mobile API" section only after a
  product decision is made — do NOT invent endpoints speculatively.
