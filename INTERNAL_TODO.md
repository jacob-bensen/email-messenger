# INTERNAL_TODO — full-stack sync routine

Known gaps and deferred work discovered by the sync routine. Ordered by
severity: contract breakage > logic drift > missing tests > style.
Consume from and append to this file every run. This file complements
(does NOT replace) `master/BACKLOG.md`, which the single-repo product
routine owns.

## Contract gaps (blocking mobile progress)

- **No mobile auth path.** The mobile app cannot log a user in. Design a
  `POST /api/v1/session` (email + password) that mints a bearer token,
  plus token verification middleware on `/api/v1/me/**`. Requires
  CSRF-disable on `/api/v1/**` for POST and a new token store table.
  Nothing to mirror on the client until this is designed.
- **No `/api/v1/me/*` endpoints.** Everything downstream of login
  (mailboxes, chats, send, notification prefs) is website-only Thymeleaf
  today; the mobile app has nothing to render past a splash screen.
  Order to build: `me` → `me/chats` (paginated rail) → `me/chats/{key}`
  (bubbles) → `me/chats/{key}/send`.
- **No push channel for mobile.** SSE at `/chats/events` is browser-only
  today (no auth for mobile). Web-push roadmap in the product
  `CLAUDE.md` is separate; native push notifications are a distinct
  problem for the mobile side.

## Local sync-audit findings

- `templates/pricing.html` almost certainly hardcodes plan prices as
  strings; they should be replaced with values from the shared
  `PlanPricing` helper (now `com.emailmessenger.billing.PlanPricing`).
  Audit `pricing.html` and thread through a view model when time
  permits.
- No REST endpoints yet emit a canonical error shape. Design one
  (`{"error": {"code": …, "message": …}}` or similar) before the second
  endpoint ships so retrofitting stays cheap.

## Tooling gaps in the sync environment

- Flutter/Dart SDK is not installed in the sync environment.
  `flutter analyze` and `flutter test` cannot be run. Do NOT claim they
  passed in a run summary. When implementing mobile changes, write Dart
  that has been walked through by eye and rely on the backend contract
  tests to catch shape mismatches.
- Consider adding a `.sessionstart.sh` (via the `session-start-hook`
  skill) that installs the Flutter SDK on session start so future runs
  can verify Dart.
