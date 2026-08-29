# INTERNAL_TODO.md — email-messenger

Running list of known gaps, tech debt, and deferred work discovered by
the full-stack sync routine or by hand. Consume the top item on the
next run; append newly discovered work at the appropriate severity.

Severity order: **contract breakage > logic drift > missing tests > style**.

## Contract breakage

- **No mobile-facing REST API exists.** The sibling repo
  `conexus-mail-mobile` will be a Flutter client, but the backend has
  no `/api/v1/**` surface today — every controller returns a Thymeleaf
  template. `docs/API.md` captures the target contract as TBD. First
  mobile-work slice must add the auth-token endpoint before anything
  else can be wired up.
- **Session-cookie auth is unsuitable for the mobile client.** Spring
  Security is configured for form login + remember-me + Google OIDC
  redirect (`com.emailmessenger.auth.SecurityConfig`). A mobile client
  needs a bearer-token issuer + a `/api/v1/**` filter chain that
  disables CSRF and uses that token; both are unimplemented.
- **Only two non-Stripe JSON endpoints exist today** — `POST /chats/refresh`
  (returns `{"imported": N}`) and `GET /chats/events` (SSE). Both
  require the session cookie and are called by the Thymeleaf page's
  own JS. The mobile client cannot reuse them until token auth lands.

## Logic drift

- (None known — there is no client yet to drift from.)

## Missing tests

- No integration test exercises `POST /chats/refresh` or
  `GET /chats/events` end-to-end. Add one before the mobile client
  starts depending on similar semantics.

## Style / housekeeping

- `docs/BACKEND.md` calls out that this project uses **JPA, not jOOQ**,
  contrary to the generic full-stack routine's default. Confirm again
  on the next run that no code accidentally references `org.jooq`.
- Website copy and mobile copy will share brand language ("ConexusMail",
  "chats" workspace). When the mobile client lands, consider extracting
  shared marketing/product strings into a single source so drift
  doesn't creep in.

## Backlog handoff from the roadmap in `CLAUDE.md`

The unchecked items in the "Roadmap" section of the repo `CLAUDE.md`
(support-email wiring, Google OAuth consent-screen publish, Live
Delivery part 2 / Web Push, delivery states for outbound bubbles,
signature-based living contact card, per-person shared files & links,
reply-to-a-specific-bubble) are all backend-only or backend-first. The
full-stack routine defers them to the existing roadmap; they don't need
to be duplicated here unless the mobile client will consume them.
