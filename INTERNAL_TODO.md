# INTERNAL_TODO

Running list of known gaps and deferred work. Consume from and append to this
every routine run. Order by severity: contract breakage > logic drift > missing
tests > style.

## Contract / architecture gaps

1. **No JSON REST API for the mobile client.** The backend is server-rendered
   Thymeleaf. `docs/API.md` §2 lists the planned surface but nothing under
   `/api/v1/*` exists. The mobile app cannot make useful authenticated calls yet.
   Next runs should add, in order:
   - Auth token issuance and validation (see #2).
   - `GET /api/v1/me` (smallest useful endpoint to prove the pipe).
   - `GET /api/v1/chats` and `GET /api/v1/chats/{key}` — reuse
     `ConversationListService` and `ConversationService` — no forked logic.
   - `POST /api/v1/chats/{key}/send` — reuse `OutboundMessageService`.
   - SSE parity at `/api/v1/events`.

2. **No token-based auth.** Only session cookies (form login + Google OIDC) exist
   today. Native clients can't share the browser session cleanly. Decision needed:
   opaque DB-backed bearer tokens (simplest) vs. adding an OAuth2 authorization
   server. Recommend opaque tokens with refresh, stored in a new `api_tokens`
   table with a `revoked_at` column. Add via a new Flyway migration
   `V36__api_tokens.sql`.

3. **Web and (future) API must share the service layer.** All existing chat/mail
   logic already lives under `service/` and `email/`; controllers are thin. When
   the mobile API lands, controllers under a new `api/` package must call the same
   service methods — not duplicate the logic. Watch for drift.

## Mobile-side gaps

4. **Flutter app is a skeleton.** `conexus-mail-mobile` now has `pubspec.yaml`,
   `lib/main.dart`, and empty `lib/domain/`, `lib/services/`, `lib/screens/`
   directories after this run. No screens, no networking, no state management
   chosen yet. First real feature will force those choices.

5. **State management not chosen.** `docs/MOBILE.md` marks this TBD. Likely
   `flutter_riverpod` or `provider` — decide when the first non-trivial screen
   lands.

## Docs / process

6. **Publish Google OAuth consent screen to production.** Backend `CLAUDE.md`
   roadmap still flags this — refresh tokens expire after 7 days in Testing mode.
   External blocker (needs Google review).

## Deferred from backend roadmap (see `CLAUDE.md`)

- Support email address + wiring.
- Live delivery, part 2 (Web Push via existing PWA service worker).
- iMessage-style delivery states on outbound bubbles.
- Signature → contact card + timeline diffs.
- Per-person shared files/links (needs blob store).
- Reply-to-a-specific-bubble threading.
