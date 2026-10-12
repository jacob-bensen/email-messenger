# BACKEND.md — architecture

Repo classification: BACKEND (Spring Boot + Flyway + Maven, Thymeleaf templates).
Sibling repo: `conexus-mail-mobile` (Flutter client; currently a bare README).

> Reverse-engineered 2026-10-12 from the code. Anything not verified is marked TBD.
> Deviation from the generic target stack: persistence is **Spring Data JPA
> (Hibernate), not jOOQ**. There is no jOOQ codegen step.

## Stack
Java 21, Spring Boot 3 (web, thymeleaf, data-jpa, validation, mail, security,
oauth2-client, actuator), Flyway (35 migrations, `V1`..`V35`), PostgreSQL in
prod/local, H2 in tests, Maven wrapper (`./mvnw`).

## Package layout (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, landing/legal/PWA/SEO), SSE (`ChatEventStream`), `ChatService`, `ConversationListService`, `OutboundMessageService` |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), `ConversationService`, `ConversationKeyService` (participant-set key), `ReplyService` |
| `email` | IMAP/Gmail integration: import, polling (`MailboxPollingService`), IDLE (`ImapIdleService`), credential encryption, MIME parsing |
| `auth` | Form login, Google OIDC, registration, password reset, email verification, account settings, login throttling, `SecurityConfig` |
| `billing` | Stripe checkout/portal/webhook (`BillingService`), plan limits (`PlanLimitService`), banners/nudges |
| `team` | Team invites/membership |
| `digest`, `reengagement`, `activation` | Scheduled emails |
| `admin` | Operator metrics (`/admin/revenue`) |
| `domain` / `repository` | JPA entities / Spring Data repositories |

## Schema overview (Flyway `src/main/resources/db/migration`)
Tables: `users`, `subscriptions`, `plan_change_events`, `mail_accounts`,
`email_threads`, `messages`, `message_recipients`, `participants`,
`attachments` (`blob_ref` always null — bytes are not stored),
`saved_searches`, `teams`, `team_members`, `team_invites`, `thread_notes`,
`auth_events`, `email_verifications`, `password_reset_tokens`,
`persistent_logins`, `digest_email_preferences`.
Plans: `FREE`, `PRO`, `BUSINESS` (`domain/Plan`).
Conversations are keyed by `conversationKey` (hash of the participant set), not
by thread; `/chats/{key}` merges all threads for that key.

## Auth model
Session-cookie auth (`JSESSIONID`) + remember-me; form login at `/login`, Google
OIDC at `/auth/google/start`. CSRF is enabled for all routes except
`/h2-console/**` and `/billing/webhook`. Public routes are the allow-list in
`auth/SecurityConfig`; everything else requires authentication.
**There is no token (JWT/API-key) auth, so a mobile client cannot authenticate
today.** See `docs/API.md` §Planned.

## Web vs REST
All controllers are server-rendered MVC (`@Controller`). The only JSON/machine
endpoints are `POST /chats/refresh` (`@ResponseBody`), `GET /chats/events`
(SSE) and `POST /billing/webhook` (`@RestController`). Controllers delegate to
services (`ChatService`, `MailAccountService`, `BillingService`, ...), so a
future `/api/v1` layer must call those same services — not controllers.

## Tests
`./mvnw test` (JUnit 5, MockMvc, Mockito; one Docker-only Testcontainers test is
skipped without Docker).
