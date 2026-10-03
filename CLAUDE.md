# email-messenger

Spring Boot web app that transforms emails into a modern instant-message-style
conversation view.

## Current status

Working SaaS ("ConexusMail"): auth (form + Google OIDC), Stripe billing
(Free/Pro/Business), multi-account mailbox connect (IMAP password + Gmail
OAuth), polling import with sent-folder sync, the texting-style /chats
workspace keyed by participant-set conversationKey, teams, saved searches,
digests, admin metrics, and a PWA shell. See the roadmap below for what's
next.

## Tech stack (target)

- Java 21 (LTS)
- Spring Boot 3.x — `spring-boot-starter-web`, `-thymeleaf`, `-data-jpa`,
  `-validation`, `-mail`
- PostgreSQL 16 in production and local dev, H2 for tests. Same setup as
  tax-tracker: base `application.yml` reads `JDBC_DATABASE_URL` /
  `JDBC_DATABASE_USERNAME` / `JDBC_DATABASE_PASSWORD` env vars,
  `application-local.yml` (`local` profile) points at
  `localhost:5432/email_messenger`, and `db/init.sql` bootstraps the
  database + app user. Tests use H2 via `src/test/resources/application.yml`.
- Thymeleaf for server-rendered views
- Maven (with wrapper) for build
- JUnit 5 + Spring Boot Test, Testcontainers for integration tests
- Flyway for schema migrations

## Commands

These will work once the Maven project is scaffolded; until then they fail.

- Build: `./mvnw clean package`
- Run locally (needs local Postgres, see `db/init.sql`):
  `JDBC_DATABASE_PASSWORD=... ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`
- Test: `./mvnw test`
- Single test: `./mvnw test -Dtest=ClassName#method`

## Target package layout

```
com.emailmessenger
├── EmailMessengerApplication.java
├── web/          # Thymeleaf controllers, form objects
├── service/      # email parsing, IM transform, threading
├── domain/       # JPA entities
├── repository/   # Spring Data repositories
└── email/        # IMAP / mailbox integration
src/main/resources/
├── templates/    # Thymeleaf views
├── static/       # CSS, JS
├── db/migration/ # Flyway migrations (V1__init.sql ...)
└── application.yml
```

## Domain model (planned)

- `EmailThread` — groups messages by `Message-ID` / `In-Reply-To` chain.
- `Message` — one email: sender, recipients, plain + html body, sent-at,
  attachments.
- `Participant` — deduped by normalized email address; display name + avatar.
- `Attachment` — filename, mime, size, blob ref.
- View model `Conversation` — messages sorted, quoted-reply text stripped,
  consecutive same-sender messages grouped into IM-style bubble runs.

## Roadmap

The routine should work the first unchecked item, then check it off in a
commit. Add follow-ups as they emerge.

- [x] Rewrite `README.md` into a proper README (description, features, setup,
      screenshots placeholder, license).
- [x] Scaffold Maven project: `pom.xml`, `mvnw`/`mvnw.cmd`, `.mvn/wrapper/`,
      `EmailMessengerApplication.java`, `application.yml` with `dev` and
      `prod` profiles.
- [x] Add starters: web, thymeleaf, data-jpa, validation, mail; runtime
      `postgresql`; test `h2`, `spring-boot-starter-test`, `testcontainers`.
- [x] Add Flyway and write `V1__init.sql` for the domain model.
- [x] Implement domain entities and Spring Data repositories.
- [x] Email-import service: parse RFC 822 via Jakarta Mail, build threads
      from `Message-ID` / `In-Reply-To` / `References`.
- [x] IM transform: strip quoted replies (`> ...`, "On … wrote:"), collapse
      consecutive same-sender messages, render basic markdown.
- [x] Thymeleaf templates: thread list, conversation view with chat bubbles,
      reply form.
- [x] CSS for the IM look: avatars, bubbles, day separators, dark mode.
- [x] IMAP polling job (`@Scheduled`) behind a feature flag.
- [x] Integration tests with Testcontainers (Postgres) + GreenMail (SMTP/IMAP).
- [x] ~~`Dockerfile` + `docker-compose.yml` (app + postgres).~~ Removed
      2026-06-11 — not deploying via Docker; run locally instead.
- [x] ~~GitHub Actions CI: build, test, cache Maven deps.~~ Removed
      2026-06-11 — tests run locally with `./mvnw test`; do not re-add CI.
- [ ] Set up the support email (a real mailbox/address users can write to, and
      wire it into the app — e.g. footer/contact links and any "need help?"
      copy). Decide the address and where outbound support mail is sent from.
- [ ] Publish the Google OAuth consent screen to production. It is still in
      Testing mode, where Google expires Gmail refresh tokens 7 days after
      issuance, so every OAuth mailbox needs a weekly "Reconnect" until the
      app is published (added 2026-07-14; reconnect flow exists at
      `/mailboxes/gmail/reconnect/{id}`).
- [x] Live delivery, part 1 (added 2026-08-09, done 2026-08-10): IMAP IDLE
      for paid-tier accounts (`ImapIdleService`, `mailbox.idle.enabled`
      flag) + SSE stream at `/chats/events` (`ChatEventStream`,
      `NewMailEvent` published by `MailboxPollingService`) + in-place chat
      updates (rail + open-pane morph in `chat.html`, wired through
      `fragments/live-refresh.html`) replacing the reload-on-new-mail
      behavior. Not yet verified against a real IMAP server that supports
      IDLE — do that when next running locally with a connected mailbox.
- [ ] Live delivery, part 2: Web Push through the existing PWA service
      worker — VAPID keys, push-subscription storage, notification opt-in on
      /account/notifications, per-conversation "Ada: message preview"
      notifications when no tab is open.
- [ ] Delivery states for outbound bubbles, iMessage-style: Sending/Sent from
      the SMTP path; Delivered when the Sent-folder import (V31 cursor) sees
      the message's Message-ID come back; Not Delivered by parsing
      mailer-daemon DSNs (multipart/report) and matching
      them to the original outbound message — render status + retry on the
      bubble, and suppress MAILER-DAEMON pseudo-conversations from the chats
      list.
- [ ] Signature → living contact card: parse the signatures
      `EmailBodyCleaner` already lifts into structured fields (phone → tel:
      link, title, company, sites) shown in the signature rail; record
      signature changes over time and surface diffs ("phone number changed")
      as subtle timeline events; backfill missing display names from parsed
      signatures.
- [ ] Per-person shared files & links: store attachment bytes (blob store;
      plan-capped via `PlanLimitKind`), download endpoint for the 📎 chips
      (today `blob_ref` is always null — attachments aren't downloadable),
      then Media/Files/Links tabs in the signature rail scoped to the
      conversationKey, with links harvested from bubble HTML at import time.
- [ ] Reply-to-a-specific-bubble: quote-reply UI targeting one message that
      uses that message's Message-ID for In-Reply-To/References
      (`ReplyService` currently always threads onto the thread's last
      message, which mis-threads replies to older bubbles); make the #thread
      badges clickable filter chips that dim the merged timeline to one
      thread.

## Conventions

- Constructor injection only; no field `@Autowired`.
- Records for DTOs and view models; entities are classes.
- Package-private by default; widen visibility only when needed.
- Tests mirror the package of the class under test in `src/test/java/...`.
- Flyway migrations are immutable once merged — add a new `V{n}__*.sql`
  rather than editing an existing one.
- Don't add comments that just restate the code. Comment only non-obvious
  *why*.
- No backwards-compatibility shims while the project is pre-1.0; change
  call sites directly.

## Working agreement for the routine

- Always work on the branch the session started on. Do not push to `main`.
- Each iteration: pick the first unchecked roadmap item, implement it,
  update this file (check the box, add follow-ups), commit, push.
- Keep commits scoped to one roadmap item where possible.
- If a step is blocked (missing decision, external credential, etc.),
  leave the box unchecked, add a `> blocked: …` note under the item, and
  move on to the next independent item.
- Run `./mvnw test` before committing once the project compiles.

## Definition of done for the MVP

A user can connect a mailbox, see imported threads rendered as IM-style
conversations, and reply inline. Tests pass via `./mvnw test` and the app
boots locally via `./mvnw spring-boot:run -Dspring-boot.run.profiles=local`
against a local PostgreSQL 16 instance (bootstrap it with `db/init.sql`).

## Related repositories

- **conexus-mail-mobile** (Flutter client, sibling repo; branch-per-session, see its CLAUDE.md).
- `docs/API.md` is the canonical client/server contract (currently: no mobile API exists, v1 is proposed only).
- `docs/BACKEND.md` architecture, `docs/WEB.md` Thymeleaf page inventory.
- Mobile docs live in the sibling repo: `docs/DESIGN.md` (roadmap), `docs/MOBILE.md`.
- Note: this backend uses JPA/Hibernate, not jOOQ. Sync-routine work follows the session-designated branch, not `master`.
- Gates: `mvn -q -B verify` (passed 2026-10-03, exit 0).
