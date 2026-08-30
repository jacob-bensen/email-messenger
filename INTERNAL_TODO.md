# INTERNAL_TODO — email-messenger (backend + website)

Running list of gaps, tech debt, and deferred work. Consumed and appended
each routine run. Ordered by severity: contract breakage > logic drift >
missing tests > style.

## Contract / architecture — cross-repo (blocks full-stack sync)

- **No mobile-facing REST API exists.** The sibling repo
  `conexus-mail-mobile` is intended as a Flutter client but currently
  contains only a `README.md`. This backend serves HTML + one JSON
  heartbeat + SSE + Stripe webhook. Building a mobile client requires a
  new JSON API layer under `com.emailmessenger.api.v1` that delegates to
  the existing service beans (auth, mailbox CRUD, chats read/send, saved
  searches). See `docs/API.md` §2 for the sketch. Every entry there is
  TBD until the controllers actually exist.
- **No non-cookie auth path.** There is no bearer-token issuance today.
  Before any mobile endpoint ships, `POST /api/v1/auth/token` must be
  added: accepts email+password OR a verified Google ID token, returns
  access + refresh tokens. Refresh token storage: reuse the existing
  auth events table (V16) or a new `V36__api_tokens.sql` — decide when
  implementing.
- **Mobile client scaffold is a scope decision.** Flutter version,
  state-management library (Riverpod vs. Bloc), whether Flame is
  actually wanted, and app icon/branding all need a human decision
  before we scaffold. See `../conexus-mail-mobile/INTERNAL_TODO.md`.
- **SSE event payload for `/chats/events` is undocumented.** Backend
  publishes but the exact JSON shape (fields, versioning) is not written
  down anywhere — document it in `docs/API.md` §1.1 the next time
  `ChatEventStream` is opened.

## Roadmap items from `CLAUDE.md` still open

Tracked in the roadmap section of `CLAUDE.md`; not restated here. Two
items are effectively blocked on the human:

- **Support email address** — needs a real mailbox and a decision on
  where outbound support mail sends from. Cannot be picked by the agent.
- **Publish Google OAuth consent screen** — external, only doable by
  the project owner in Google Cloud console.

The remaining roadmap items (Web Push, delivery states for outbound
bubbles, signature living contact card, attachment blob store,
reply-to-a-specific-bubble) are all doable in-repo when the routine
picks them up.

## Ops / deploy backlog

Everything in `master/MASTER_ACTIONS.md` — Stripe keys, Postgres, hosting
platform choice, domain, mailbox encryption secrets, transactional email
provider. Not restated here.

## Docs

- `docs/BACKEND.md`, `docs/API.md`, `docs/WEB.md` were bootstrapped on
  2026-08-30. Keep them accurate: any new controller, route, or DTO
  change updates the doc in the same commit.
- The `master/` planning tree (PLAN.md, BACKLOG.md, MASTER_ACTIONS.md,
  CHANGELOG.md) predates the `docs/` tree. It is authoritative on
  business priorities (win-back / EPIC-18); `docs/` is authoritative on
  code and contracts. Do not duplicate content between them — link
  instead.
