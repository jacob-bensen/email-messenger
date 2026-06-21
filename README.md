# email-messenger

Turn your inbox into a chat. **email-messenger** is a Spring Boot web app
that imports email threads and renders them as a modern instant-message
conversation — bubbles, avatars, day separators, dark mode — instead of
the nested-quote mess most mail clients show you.

> **Status:** in development. The MVP is being scaffolded; see
> [`CLAUDE.md`](./CLAUDE.md) for the roadmap and current progress.

## Features

- **IM-style conversation view** — messages in a thread are grouped by
  sender, sorted by time, and rendered as chat bubbles.
- **Quoted-reply stripping** — collapses `> …` blocks and
  "On … wrote:" preambles so each message shows only what's new.
- **Header-based threading** — groups messages by `Message-ID` /
  `In-Reply-To` / `References`, not subject matching.
- **Inline reply** — reply from inside the conversation; the message is
  sent via SMTP and threaded back into the same conversation.
- **Mailbox sync** — scheduled IMAP poll pulls new messages.
- **Attachments** — preserved per message with download links.
- **Dark mode** — follows the system preference.

## Tech stack

- Java 21, Spring Boot 3
- PostgreSQL 16 (H2 in tests), Flyway migrations
- Thymeleaf views, vanilla CSS for the IM look
- Jakarta Mail for IMAP / SMTP
- Maven (with wrapper)
- JUnit 5, Spring Boot Test, Testcontainers, GreenMail

## Getting started

### Prerequisites

- Java 21
- PostgreSQL 16 running locally

### Run locally

Bootstrap the database and app user once (statements in
[`src/main/resources/db/init.sql`](src/main/resources/db/init.sql)):

```bash
psql -U postgres -f src/main/resources/db/init.sql
```

Then start the app with the `local` profile, supplying the database
password you chose in `init.sql`:

```bash
JDBC_DATABASE_PASSWORD=password ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The `local` profile points at `jdbc:postgresql://localhost:5432/email_messenger`
as `email_messenger_app`; Flyway runs the migrations on startup. In
production the base config reads the database connection entirely from
environment variables (see the table below and `.env.example`).

The app starts on `http://localhost:8080`. On first launch you create an
account, then connect a mailbox (IMAP host, port, username, app password).

### Tests

```bash
./mvnw test
```

The Testcontainers-based integration tests need a local Docker daemon;
they skip automatically when Docker isn't available.

### Build a jar

```bash
./mvnw clean package
java -jar target/email-messenger-*.jar
```

## Configuration

`application.yml` holds sensible defaults; override them via environment
variables (see [`.env.example`](./.env.example)). Anything not listed below
falls back to a working default and needs no configuration.

**Required** — the app won't boot or core features break without these:

| Setting             | Environment variable     | Default / local-profile value                      |
| ------------------- | ------------------------ | -------------------------------------------------- |
| Database URL        | `JDBC_DATABASE_URL`      | `jdbc:postgresql://localhost:5432/email_messenger` |
| Database user       | `JDBC_DATABASE_USERNAME` | `email_messenger_app`                              |
| Database password   | `JDBC_DATABASE_PASSWORD` | (set in `db/init.sql`)                             |
| SMTP host / port    | `MAIL_HOST` / `MAIL_PORT`| `localhost` / `587`                                |
| SMTP user / password| `MAIL_USER` / `MAIL_PASS`| (blank — set for outbound replies & notifications) |
| Mailbox credential encryption | `MAILBOX_ENCRYPTION_PASSWORD` / `MAILBOX_ENCRYPTION_SALT` | (dev fallback key if unset — **set in prod**, else stored IMAP/SMTP passwords use a public key) |

**Optional** — feature toggles, off/defaulted unless set:

| Setting             | Environment variable     | Default | Effect when set                          |
| ------------------- | ------------------------ | ------- | ---------------------------------------- |
| Google sign-in      | `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | blank | Enables "Continue with Google" (blank hides it) |
| Stripe billing      | `STRIPE_SECRET_KEY` / `STRIPE_WEBHOOK_SECRET` | blank | Enables checkout & subscription sync (inert if blank) |
| IMAP polling        | `MAILBOX_POLLING_ENABLED`| `true`  | Background poll of connected mailboxes   |

The `/admin/**` operator dashboard is gated by an allowlist hardcoded in
`application.yml` (`admin.emails`) — no env var required; comma-separate to add
operators. A non-admin hitting an admin URL gets a 404.

### Billing (Stripe)

Billing activates once `STRIPE_SECRET_KEY` and `STRIPE_WEBHOOK_SECRET` are set.
The webhook signing secret is **per endpoint** (per receiving URL), so each
deployment registers its own endpoint and uses that endpoint's `whsec_…`.

Point the Stripe webhook endpoint at `POST /billing/webhook` and subscribe to
exactly these four events — the app mirrors them onto local subscription rows
and ignores everything else:

| Event                            | Handling                                                        |
| -------------------------------- | -------------------------------------------------------------- |
| `checkout.session.completed`     | Links the Stripe subscription to the user and sets initial status. |
| `customer.subscription.created`  | Mirrors status, plan/price, trial end, and period end.         |
| `customer.subscription.updated`  | Same mirroring — plan changes, renewals, status flips.         |
| `customer.subscription.deleted`  | Marks the local subscription canceled.                         |

For local testing, forward events with the Stripe CLI instead of a dashboard
endpoint:

```bash
stripe listen --forward-to localhost:8080/billing/webhook
```

It prints a temporary `whsec_…` to use as `STRIPE_WEBHOOK_SECRET`.

## Screenshots

_Coming once the conversation view ships._

## Project layout

See [`CLAUDE.md`](./CLAUDE.md) for the target package layout, domain
model, and development conventions.

## License

TBD.
