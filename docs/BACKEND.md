# Backend architecture

Spring Boot 3 / Java 21 monolith ("ConexusMail", repo `email-messenger`). It serves the
public website and the signed-in web app via Thymeleaf. **There is no REST API for the
mobile client yet** — see `docs/API.md`.

> Classification note: this repo is the BACKEND (pom.xml, Spring annotations, Flyway
> migrations). Deviation from the generic routine template: persistence is Spring Data
> **JPA** (Hibernate, `ddl-auto: validate`), not jOOQ; there is no jOOQ codegen.

## Packages (`com.emailmessenger`)

| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, pricing/legal/SEO/PWA), `ChatService`, `ConversationListService`, `OutboundMessageService`, SSE `ChatEventStream` |
| `service` | Email → IM transform: `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`, view records (`ChatConversation`, `BubbleRun`, …) |
| `email` | Mailbox integration: IMAP import (`EmailImportService`, `MimeMessageParser`), polling (`MailboxPollingService/Scheduler`), IMAP IDLE (`ImapIdleService`), Gmail OAuth, credential encryption |
| `domain` | JPA entities (`User`, `MailAccount`, `EmailThread`, `Message`, `Participant`, `Attachment`, `Subscription`, `Team*`, `SavedSearch`, …) |
| `repository` | Spring Data repositories |
| `auth` | Spring Security config, registration/login, Google OIDC, password reset, email verification, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), trial nudges |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, weekly digests, lifecycle emails, operator metrics (`/admin/**`) |

## Data model (Flyway V1–V35, `src/main/resources/db/migration`)

Tables: `users`, `mail_accounts`, `email_threads`, `messages`, `message_recipients`,
`participants`, `attachments` (`blob_ref` always null today), `thread_notes`,
`saved_searches`, `subscriptions`, `plan_change_events`, `teams`, `team_members`,
`team_invites`, `auth_events`, `password_reset_tokens`, `email_verifications`,
`digest_email_preferences`, `persistent_logins`.

Conversations are **not** a table: a chat is the set of threads sharing a
participant-set `conversationKey` (`ConversationKeyService`).

## Auth model

Session cookie (`JSESSIONID`) via Spring Security form login (`/login`, params `email`,
`password`) or Google OIDC (`/auth/google/start`); persistent remember-me (30 days, JDBC
token repo). **CSRF is enabled** (only `/billing/webhook` and `/h2-console/**` exempt).
Everything not whitelisted in `SecurityConfig` requires authentication. No bearer/JWT
mechanism exists — a mobile client cannot authenticate non-interactively today.

## Web ↔ API relationship

Controllers return view names and talk to services/repositories directly. Several
controller methods contain orchestration that a REST API would need to reuse
(see `INTERNAL_TODO.md`: `ConversationController` recipient parsing, sender selection,
signature appending, send+record sequence). Extract to a service before adding REST.

## Build / run / test

See `CLAUDE.md`. Tests: JUnit 5 + H2 (+ GreenMail/Testcontainers integration tests).
