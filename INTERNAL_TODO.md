# Internal TODO

Living list of known gaps, tech debt, and deferred work discovered by the
full-stack sync routine. Ordered by severity within each section.

## Contract breakage (highest)

- **No JSON API for the mobile client.** All controllers today return
  Thymeleaf HTML. `conexus-mail-mobile` cannot integrate. First feature
  run must scaffold `/api/v1/**`, decide an auth model
  (personal-access-token vs. OAuth2 password grant), and ship at least
  `POST /api/v1/auth/token` + `GET /api/v1/me` + `GET /api/v1/chats`
  matching `docs/API.md`.

## Logic drift risks

- `ConversationController` and `ReplyService` currently own reply-header
  building via the "reply to last message on the thread" shortcut. Any
  future JSON `POST /api/v1/chats/{key}/send` MUST route through the same
  service, not re-implement threading. See the "reply-to-a-specific-bubble"
  roadmap item — that refactor should land before the mobile endpoint.
- Live SSE lives at `/chats/events`, browser-shaped. The mobile
  `/api/v1/events` endpoint should share `ChatEventStream` (per-user
  publish/subscribe) but produce a mobile-shaped payload — extract a
  common publisher rather than forking.

## Auth model open questions

- Session cookies (browser) vs. bearer tokens (mobile) — decide the token
  issuance flow (see `docs/API.md`).
- Google OIDC on mobile: web flow with system browser + PKCE, or native
  Google Sign-In SDK? Deferred until first mobile auth work.
- Email verification gate — mobile client must handle the "not verified
  yet" state without a browser round-trip.

## Missing tests

- No slice tests for the (still-empty) JSON API. Add
  `@WebMvcTest`-shaped tests alongside each `/api/v1/**` controller as it
  lands.

## Style / small

- The generic full-stack routine instructs work on `master`; this repo's
  session-issued branch is `claude/relaxed-newton-ahk5io`. Runs must
  follow the branch requirement (harness-issued), not the routine's
  default.

## Repository scope reminder

Sibling repo: `jacob-bensen/conexus-mail-mobile` (branch
`claude/hopeful-einstein-ahk5io`). Do not read or push outside these two.
