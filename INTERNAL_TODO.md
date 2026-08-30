# INTERNAL_TODO.md — backend

Running punch-list for the two-repo sync routine. Ordered by severity:
contract breakage > logic drift > missing tests > style. Add items as
you find them; strike (delete) them when done.

## Contract breakage / gaps

- **[HIGH] No REST API exists.** The Flutter client can call nothing.
  Bootstrap order laid out in `docs/API.md`: mobile-auth → chats-list
  → chat-pane → send → SSE. Every mobile feature is blocked until at
  least the first four exist.
- **[HIGH] No bearer-token auth path.** Add `mobile_session` table
  (opaque token, user_id, expires_at, revoked_at) + `MobileAuthFilter`.
  Wire it BEFORE any `/api/v1/**` endpoint so we don't accidentally
  ship an unauthenticated JSON API.
- **[MED] CSRF is on for all POSTs.** When adding `/api/v1/**`, scope
  `CsrfConfigurer#ignoringRequestMatchers` to that prefix — do NOT
  disable globally.

## Logic drift risks

- **[MED] Web `/chats/**` logic lives partly in `ConversationController`.**
  Before mirroring it into an API endpoint, extract the branchy view
  building into a dedicated `ChatViewService` so both callers share
  the same code path. If we don't, the mobile app will re-implement
  quirks like consecutive-same-sender bubble merging separately.
- **[MED] Signature parsing.** `EmailBodyCleaner` already extracts
  fields the roadmap wants shown as a "living contact card"; those
  parsed fields aren't persisted per-signature-version yet. Any mobile
  contact-card work depends on this being solidified server-side first.

## Missing tests

- No integration test that boots the app and asserts a JSON response
  from `/api/v1/**` — trivially none exist because the surface doesn't.
  Add a `@SpringBootTest` slice alongside the first endpoint.

## Deferred product work already tracked elsewhere

- Roadmap items in `CLAUDE.md` (support email, Google OAuth
  publish-to-prod, live delivery part 2 / Web Push, iMessage delivery
  states, per-person shared files, quote-reply-to-bubble). Not
  duplicated here — the main-repo CLAUDE.md file owns them.
- Human-blocked deploy items in `master/MASTER_ACTIONS.md`.

## Style / repo hygiene

- `master/` directory at the repo root is unusual. Kept intact because
  earlier sessions treat it as canonical planning storage. Not moving
  it without explicit user approval.
