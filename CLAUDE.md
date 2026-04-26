# email-messenger

Spring Boot web app that transforms emails into a modern instant-message-style
conversation view.

## Current status

Greenfield. The repo contains only `README.md`, `.gitignore`, and this file.
No `pom.xml`, source code, or build configuration exists yet — the next
session has to scaffold the project before anything else compiles or runs.

## Tech stack (target)

- Java 21 (LTS)
- Spring Boot 3.x — `spring-boot-starter-web`, `-thymeleaf`, `-data-jpa`,
  `-validation`, `-mail`
- PostgreSQL 16 in production, H2 for tests
- Thymeleaf for server-rendered views
- Maven (with wrapper) for build
- JUnit 5 + Spring Boot Test, Testcontainers for integration tests
- Flyway for schema migrations

## Commands

These will work once the Maven project is scaffolded; until then they fail.

- Build: `./mvnw clean package`
- Run: `./mvnw spring-boot:run`
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
- [ ] IMAP polling job (`@Scheduled`) behind a feature flag.
- [ ] Integration tests with Testcontainers (Postgres) + GreenMail (SMTP/IMAP).
- [ ] `Dockerfile` + `docker-compose.yml` (app + postgres).
- [ ] GitHub Actions CI: build, test, cache Maven deps.

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
conversations, and reply inline. Tests pass and the app boots against
Postgres in Docker via `docker compose up`.
