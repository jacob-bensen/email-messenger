# INTERNAL_TODO.md — email-messenger

Running list of known gaps, tech debt, and deferred decisions
consumed and appended by the full-stack sync routine. Severity
ordering (worst first): contract breakage → logic drift → missing
tests → style.

## Contract / cross-repo

- **Mobile auth mechanism is undecided.** The Thymeleaf site uses
  `JSESSIONID` cookies and Spring Security form login. The mobile
  app cannot ride cookies. Options: (a) issue a signed bearer token
  on successful form login; (b) add `POST /api/v1/auth/token` that
  takes email+password (bcrypt-verified) and returns a JWT/opaque
  token; (c) accept a Google ID token from the mobile Google
  Sign-In SDK and verify it server-side. Recommend (b) + (c) as
  parallel entry points; defer to next run once the mobile side
  scaffolds. Blocks every authenticated endpoint in `docs/API.md`.
- **Live delivery on mobile is undecided.** Web uses SSE at
  `/chats/events`. SSE dies when an iOS app backgrounds. Options:
  short-lived WebSocket while the app is foregrounded + APNs / FCM
  push while backgrounded. Depends on push infra (also a roadmap
  item on the backend: Web Push).
- **DTO drift risk once the API grows.** Only `StatusResponse` exists
  today, mirrored by hand in the client. Before there are more than
  ~3 endpoints, decide whether to hand-write DTOs on both sides,
  generate the client from an OpenAPI spec, or ship a shared JSON
  schema. Recommend hand-written until 6 endpoints; then revisit.

## Backend

- Attachment `blob_ref` is always null — attachments aren't
  downloadable yet. Already tracked on the backend roadmap
  (`CLAUDE.md`); noted here because the mobile client will need the
  same download endpoint.
- `MailboxIdleService` (paid-tier IMAP IDLE) has not been verified
  against a real IMAP server (tracked in `CLAUDE.md` roadmap;
  duplicated here so the sync routine sees it).
- Google OAuth consent screen is still in Testing mode → Gmail
  refresh tokens expire weekly. Track until published.

## Frontend

- Flutter project is a bare skeleton — no state management picked,
  no networking layer beyond a hand-written client for
  `/api/v1/status`, no auth. Everything downstream of "call one
  endpoint" is TBD.
- `lib/domain/` mirrors nothing meaningful yet because the API
  surface is one status ping. Grow it alongside real endpoints.

## Docs / process

- Ran a first-pass sync audit on 2026-08-31: no contract mismatches
  because the contract is one endpoint. Re-audit after the second
  authenticated endpoint lands.
- `docs/DESIGN.md`'s roadmap and `email-messenger/CLAUDE.md`'s
  roadmap will drift. Decide which is the source of truth for
  mobile features (should be `DESIGN.md`) vs backend features
  (should be `CLAUDE.md`) and cross-link.
