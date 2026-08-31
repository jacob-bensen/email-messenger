# INTERNAL_TODO — email-messenger

Running list of known gaps, tech debt, and deferred work. Ordered by
severity: contract breakage > logic drift > missing tests > style.

## Contract breakage (mobile client cannot exist without these)

- **No JSON API layer.** The server today is entirely Thymeleaf-rendered
  HTML. Every endpoint in `docs/API.md` is marked ❌ planned. First
  vertical slice: `POST /api/v1/auth/sign-in`, `GET /api/v1/chats`,
  `GET /api/v1/chats/{key}`, `POST /api/v1/chats/{key}/messages`. Add
  `/api/**` matcher to `SecurityConfig` with bearer-token auth (no
  session cookie), CSRF disabled on that matcher.
- **No bearer-token / refresh-token infrastructure.** Need issuance,
  storage (`api_tokens` table with hashed token + expiry + refresh),
  rotation on refresh, revocation on sign-out. Design decision needed
  on whether to reuse Spring Security's OAuth2 resource-server support
  or hand-roll a simple opaque-token authenticator.
- **CSRF and cookie-based session are hard-baked into `SecurityConfig`.**
  Adding `/api/**` needs a second `SecurityFilterChain` bean with a
  different `securityMatcher` and filter chain — do NOT try to bolt
  bearer auth onto the existing chain.

## Logic drift risks

- **Business logic re-implementation.** When the REST layer lands, its
  controllers MUST call the exact same service methods the Thymeleaf
  controllers call today (`ConversationService`, `ReplyService`,
  `MailAccountService`, `MailboxPollingService.pollNow(...)`, etc.).
  Any new business logic added in the REST controllers is a bug.
- **Session-only rate limiting.** `LoginThrottleFilter` runs in the form
  login chain only. The mobile `POST /api/v1/auth/sign-in` will need
  the same throttling — extract the counter into `AuthEventService`
  and reuse it, don't duplicate the counting logic.

## Missing behavior on the roadmap (still open in CLAUDE.md)

Tracked in the top-level `CLAUDE.md` roadmap — reproduced here so the
sync routine doesn't miss them:

- [ ] Set up the support email (real mailbox + wire into footer/help copy).
- [ ] Publish Google OAuth consent screen to production (blocks 7-day
  Gmail refresh-token expiry workaround).
- [ ] IMAP IDLE verification against a real IDLE-capable server.
- [ ] Live delivery, part 2: Web Push (VAPID keys, `push_subscriptions`
  table, opt-in on `/account/notifications`).
- [ ] Delivery states for outbound bubbles (Sending/Sent/Delivered/Not
  Delivered) via SMTP path + Sent-folder cursor + DSN parsing.
- [ ] Signature → living contact card (parse structured fields, timeline
  diffs, backfill display names).
- [ ] Per-person shared files & links (attachment blob store,
  downloadable 📎 chips, Media/Files/Links tabs).
- [ ] Reply-to-a-specific-bubble (use targeted `Message-ID` in the
  `In-Reply-To`/`References` chain instead of always the thread's last
  message).

## Missing tests

- No test covers `/chats/events` SSE end-to-end (event fires + client
  receives) beyond `ChatEventStreamTest`; add an integration test that
  drives `MailboxPollingService.pollAll()` and asserts the SSE emitter
  received a `new-mail` event.
- IMAP IDLE service has no integration test — CLAUDE.md flags this.
- No test for `ConversationKeyBackfill` behavior when a thread has zero
  participants (degenerate case).

## Style / cleanup

- `master/` at the repo root holds coordination docs (`PLAN.md`,
  `BACKLOG.md`, `MASTER_ACTIONS.md`, `CHANGELOG.md`) for a separate
  autonomous "Master" routine that also drives this repo. It is
  intentionally tracked; leave it alone from this routine.
- `Dockerfile` and `docker-compose.yml` are present but CLAUDE.md
  roadmap says Docker was removed 2026-06-11. Confirm whether the
  files should be deleted or the CLAUDE.md note is stale.
