# BACKEND.md — architecture

Reverse-engineered 2026-10-10 from the code. Anything not verified is marked TBD.

## Stack (actual)
Java 21, Spring Boot 3, Spring Data **JPA/Hibernate** (`ddl-auto: validate`), Flyway
(35 migrations, `V1`–`V35`), PostgreSQL (prod/local), H2 (tests), Thymeleaf, Maven wrapper.
**There is no jOOQ** — the generic routine template assumes it; this repo does not use it.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `domain` | JPA entities: `User`, `MailAccount`, `EmailThread`, `Message`, `MessageRecipient`, `Participant`, `Attachment`, `Subscription`, `Plan`, `Team*`, `SavedSearch`, `AuthEvent`, tokens |
| `repository` | Spring Data repositories |
| `email` | IMAP/Gmail import: `MimeMessageParser`, `EmailImportService`, `MailboxPollingService` (+scheduler), `ImapIdleService`, `GmailOAuthClient`, `CredentialEncryptor` |
| `service` | Chat model: `ConversationKeyService`, `IMTransformService`, `EmailBodyCleaner`, `ReplyService`, `ConversationService` |
| `web` | Controllers + view services (`ChatService`, `ConversationListService`, `OutboundMessageService`, `ChatEventStream`) |
| `auth` | Spring Security config, registration, password reset, email verification, Google OIDC, login throttle |
| `billing` | Stripe checkout/portal/webhook, `PlanLimitService` (Free/Pro/Business caps) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | teams/invites, weekly digest, lifecycle emails, revenue/funnel metrics |

## Core concepts
- **conversationKey** (`email_threads.conversation_key`): participant-set key. All email threads
  with the same people merge into one chat timeline.
- **Server authority:** the server imports mail, owns threading, quoted-reply stripping, plan limits.
- **Plans:** Free/Pro/Business enforced by `PlanLimitService` (throws `PlanLimitExceededException`).

## Auth model
Session cookie (`JSESSIONID`) via form login (`POST /login`, params `email`, `password`) or Google
OIDC (only if `GOOGLE_CLIENT_ID` set); remember-me cookie (30 d, persistent_logins table). **CSRF is
enabled** for every POST except `/billing/webhook` and `/h2-console/**`. No bearer-token / API-key
auth exists. Everything not on the public allow-list in `SecurityConfig` requires authentication;
unauthenticated requests redirect to `/login` (HTML), not 401 JSON.

## Web vs. API
There is **no REST API for a mobile client**. All user-facing behaviour is server-rendered
Thymeleaf (see WEB.md). The only JSON/stream endpoints are `POST /chats/refresh` and
`GET /chats/events` (SSE). Both web and any future API must call the same services
(`ChatService`, `ConversationListService`, `OutboundMessageService`, `MailAccountService`);
today controllers `ConversationController` still hold some orchestration (mailbox scoping,
recipient parsing, `selectMailbox`) that a REST layer would need extracted — see INTERNAL_TODO.md.
