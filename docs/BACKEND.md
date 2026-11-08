# BACKEND — architecture overview

Repo: `email-messenger` (product "ConexusMail"). Classified BACKEND by evidence:
`pom.xml`, Spring Boot annotations, Flyway migrations (`V1`–`V35`), Thymeleaf templates.

## Stack (as implemented)
Java 21, Spring Boot 3 (web, thymeleaf, **data-jpa** — *not* jOOQ, security,
oauth2-client, validation, mail, actuator), PostgreSQL 16 (H2 in tests), Flyway, Maven,
Jakarta Mail (IMAP/SMTP), jsoup, Stripe SDK. Tests: JUnit 5, GreenMail, Testcontainers.

## Packages (`com.emailmessenger.*`)
| Package | Role |
|---|---|
| `auth` | form login + Google OIDC, registration, email verification, password reset, account settings, login throttling, remember-me |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`), banners, trial nudges |
| `email` | IMAP client + polling (`MailboxPollingService`), IMAP IDLE (`ImapIdleService`), MIME parsing, import/threading (`EmailImportService`), Gmail OAuth, credential encryption |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), conversations, `ReplyService`, conversation keys |
| `web` | Thymeleaf controllers (chats, mailboxes, pricing/landing, legal, PWA, SEO), SSE `ChatEventStream` |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `team`, `digest`, `activation`, `reengagement`, `admin` | teams/invites, digests, lifecycle emails, operator metrics |

## Schema overview (Flyway, immutable once merged)
`users`, `subscriptions`, `plan_change_events`, `mail_accounts`, `email_threads`,
`messages`, `message_recipients`, `participants`, `attachments` (`blob_ref` always null:
bytes not stored), `saved_searches`, `teams`/`team_members`/`team_invites`,
`auth_events`, `email_verifications`, `password_reset_tokens`, `persistent_logins`,
`digest_email_preferences`, `thread_notes`. Conversations are keyed by a
participant-set `conversationKey` (`ConversationKeyService`).

## Auth model
Server-side session (`JSESSIONID`) via form login or Google OIDC, optional persistent
remember-me cookie, **CSRF enabled** (exempt: `/billing/webhook`, `/h2-console/**`).
Everything not on the permit-list in `auth/SecurityConfig` requires authentication.
There is **no token/bearer auth**, so a mobile client cannot authenticate yet (see
`INTERNAL_TODO.md`).

## Web vs REST
All HTTP surface today is server-rendered Thymeleaf (see `WEB.md`) plus three
non-HTML endpoints: `POST /chats/refresh` (JSON), `GET /chats/events` (SSE),
`POST /billing/webhook` (Stripe). No `/api/**` exists. When one is added it must call
the same services the controllers use (`ConversationListService`, `ChatService`,
`OutboundMessageService`, `MailAccountService`, …).
