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
- Docker (for the Postgres container)

### Run locally

```bash
docker compose up -d postgres
./mvnw spring-boot:run
```

The app starts on `http://localhost:8080`. On first launch you connect a
mailbox (IMAP host, port, username, app password).

### Tests

```bash
./mvnw test
```

### Build a jar

```bash
./mvnw clean package
java -jar target/email-messenger-*.jar
```

## Configuration

Override defaults in `application.yml` or via environment variables:

| Setting             | Environment variable              | Default                                                |
| ------------------- | --------------------------------- | ------------------------------------------------------ |
| Database URL        | `SPRING_DATASOURCE_URL`           | `jdbc:postgresql://localhost:5432/email_messenger`     |
| Database user       | `SPRING_DATASOURCE_USERNAME`      | `postgres`                                             |
| Database password   | `SPRING_DATASOURCE_PASSWORD`      | `postgres`                                             |
| IMAP poll interval  | `EMAIL_MESSENGER_POLL_INTERVAL`   | `5m`                                                   |

## Screenshots

_Coming once the conversation view ships._

## Project layout

See [`CLAUDE.md`](./CLAUDE.md) for the target package layout, domain
model, and development conventions.

## License

TBD.
