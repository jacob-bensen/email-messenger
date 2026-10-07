# Backend architecture

Classification: BACKEND = `email-messenger` (pom.xml, Spring Boot, Flyway,
35 migrations). Frontend = `conexus-mail-mobile` (currently README only).

**Deviation from the generic target stack:** persistence is Spring Data JPA
(Hibernate, `ddl-auto: validate`), NOT jOOQ; there is no codegen step.

## Modules (`com.emailmessenger`)
`auth` (login, Google OIDC, password reset, verification, throttling),
`billing` (Stripe checkout/portal/webhooks, plan limits), `email` (IMAP/Gmail
import, polling, IDLE, MIME parsing), `service` (IM transform, conversations,
replies), `web` (controllers, chat workspace, mailboxes, PWA, SEO), `domain`
(JPA entities), `repository`, `team`, `digest`, `admin`, `activation`,
`reengagement`.

## Schema
`src/main/resources/db/migration/V1..V35`, immutable once merged. Postgres 16
in prod/local, H2 in tests.

## Auth
Cookie session + CSRF, remember-me (30 days, JDBC token store), BCrypt.
Mailbox credentials encrypted by `CredentialEncryptor`. No token/API auth yet.

## Web vs API
Web pages call services directly. There is no REST layer, so no web/API
logic duplication exists yet; a future `/api/v1` must reuse the same services
(`ConversationListService`, `ReplyService`, `MailAccountService`, ...).
See `docs/WEB.md` and `docs/API.md`.
