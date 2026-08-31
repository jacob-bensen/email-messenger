# Internal TODO — email-messenger

Running list of known gaps, tech debt, and deferred work. The full-stack
sync routine consumes from and appends to this file each run. Ordered
by severity: contract breakage > logic drift > missing coverage > style.

## Contract / mobile client

- **No mobile auth yet.** The mobile client has no way to log in. Design
  the session-token model (probably a header-carried bearer minted from
  an email+password POST) and add:
    - `POST /api/v1/auth/login` (email + password → token + basic user
      profile)
    - `POST /api/v1/auth/logout` (invalidate)
    - A Spring Security filter that consumes the token and populates the
      SecurityContext, sitting alongside the form-login chain.
- **No mobile chats/messages endpoints.** Once auth lands:
    - `GET  /api/v1/me`
    - `GET  /api/v1/mailboxes`
    - `GET  /api/v1/chats?after=&limit=`
    - `GET  /api/v1/chats/{key}`
    - `POST /api/v1/chats/{key}/reply` (mirrors `ReplyService`)
    - `GET  /api/v1/chats/events` (SSE, mirrors the website's stream)
  Each must be documented in `docs/API.md` in the same run it lands.
- **No JSON error handler on `/api/v1/**` yet.** `GlobalExceptionHandler`
  today renders Thymeleaf error views. Add a `@RestControllerAdvice`
  scoped to `com.emailmessenger.api` that emits the
  `{error, message}` shape declared in `docs/API.md`.
- **No mobile client version enforcement.** `minMobileBuild` is
  hardcoded to `1` in `MetaController`. Once we ship a build we might
  need to force-update, promote this to a property (env-var driven)
  and cover it with a test.

## Backend cleanup / debt (surfaced during Phase 0 audit)

- The routine's target-stack description says jOOQ; this project uses
  Spring Data JPA. Documented in `docs/BACKEND.md` — no migration
  planned. If a future item wants jOOQ, treat it as a separate
  architecture change, not "fixing drift."
- `application.yml` sets `mail.host` / port defaults that only apply
  when the SMTP env vars aren't set. Consider failing fast in prod when
  SMTP is unconfigured — right now silent no-ops during password reset /
  digest are possible.
- `EmailMessengerApplication` declares a growing list of
  `@EnableConfigurationProperties({...})`. Once it crosses ~10 classes,
  break it up (e.g. move each `*Properties` to `@ConfigurationPropertiesScan`).

## Roadmap items still open (see backend `CLAUDE.md`)

- Support email address + wiring.
- Publish Google OAuth consent screen (removes the 7-day refresh-token
  expiry).
- Live delivery, part 2 (Web Push).
- Delivery states for outbound bubbles (Sending / Sent / Delivered / Not
  Delivered via mailer-daemon DSN parsing).
- Signature → living contact card.
- Per-person shared files & links (needs a blob store).
- Reply-to-a-specific-bubble (`ReplyService` currently always threads
  onto the thread's last message).

## Test coverage gaps

- `MetaController` has a single happy-path test. Add a test with
  `auth.google.client-id` set (via `@TestPropertySource`) to prove the
  `features.googleOAuth` flag flips true when configured.
- No integration test asserts that `/api/v1/**` is CSRF-exempt for POST
  — add one when the first POST endpoint lands.

## Known blocked

- IMAP IDLE not yet verified against a real IMAP server that supports
  IDLE (roadmap note carried over from 2026-08-10).
