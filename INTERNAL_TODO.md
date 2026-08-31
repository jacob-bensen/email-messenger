# Internal TODO

Running list of gaps and tech debt for this repo, ordered by severity
per the sync routine: contract breakage > logic drift > missing tests >
style.

## Contract breakage

- **No REST API exists for the mobile client.** `conexus-mail-mobile`
  is the designated sibling but has no code yet, and this backend
  exposes only Thymeleaf pages, Stripe webhook, PWA/SEO assets, and the
  SSE stream at `/chats/events`. Before the sync routine's Phase 1 can
  do useful cross-repo work, a decision is needed on:
  - Whether a mobile client is actually in scope for the product
    roadmap. (`CLAUDE.md`'s roadmap doesn't currently list one.)
  - The auth story for a mobile client (share the web session cookie
    vs. issue bearer tokens on a new `/api/v1/auth/*` endpoint).
  - The initial endpoint surface (mirror `/chats`, `/chats/{key}`,
    `/chats/{key}/send`, `/chats/{key}/pane`, `/chats/events` first).

## Logic drift

- None identified yet — every controller delegates through service
  classes and there's no parallel implementation to drift from.

## Missing tests

- No mobile-facing integration tests (no API to test against).
- The IMAP IDLE path (`ImapIdleService`, `mailbox.idle.enabled`) is
  still "not yet verified against a real IMAP server" per the roadmap.

## Style / smaller items

- Publish the Google OAuth consent screen to production so Gmail refresh
  tokens stop expiring after 7 days (open roadmap item).
- Wire the support email into the app once decided (open roadmap item).
