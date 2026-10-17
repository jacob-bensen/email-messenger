# BACKEND.md — architecture (reverse-engineered 2026-10-17)

Repo role: **BACKEND** (evidence: `pom.xml`, Spring Boot, 35 Flyway migrations, `@Controller`s, Thymeleaf templates).
Sibling: `jacob-bensen/conexus-mail-mobile` (FRONTEND, Flutter; currently an empty scaffold).

## Stack (actual — differs from the generic routine assumptions)
- Java 21, Spring Boot 3, Maven wrapper (`./mvnw`).
- Persistence: **Spring Data JPA / Hibernate** (`ddl-auto: validate`), NOT jOOQ. There is no jOOQ codegen step.
- PostgreSQL in prod/local, H2 in tests. Flyway migrations `src/main/resources/db/migration/V1..V35` (immutable once merged).
- Thymeleaf server-rendered web UI; Spring Security (form login, Google OIDC, remember-me, CSRF on).
- Stripe billing, Jakarta Mail IMAP/SMTP, Gmail OAuth.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`ConversationController` = `/chats`, `MailboxController`, `GmailConnectController`, `SavedSearchController`, PWA/SEO/legal), `ChatService`, `ConversationListService`, `OutboundMessageService`, SSE `ChatEventStream` |
| `service` | IM transform, `ConversationService`, `ReplyService` (SMTP send), `ConversationKeyService`, `EmailBodyCleaner`, view-model records (`ChatConversation`, `ChatMember`, `ConversationListItem`, `BubbleRun`, `BubbleMessage`) |
| `email` | IMAP import (`EmailImportService`, `MimeMessageParser`), polling (`MailboxPollingService/Scheduler`), IMAP IDLE (paid), `MailAccountService`, `CredentialEncryptor` |
| `domain` / `repository` | JPA entities (`User`, `MailAccount`, `EmailThread`, `Message`, `Participant`, `Attachment`, `Subscription`, `Team*`, `SavedSearch`, ...) and Spring Data repos |
| `auth` | Registration/login/OAuth/password reset/email verification/account settings; `SecurityConfig` |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimits`: FREE 3 mailboxes/500 threads, PRO 5/unlimited, BUSINESS unlimited) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | teams/invites, digest emails, lifecycle emails, operator metrics |

## Core concept: conversationKey
Conversations in `/chats` are keyed by the participant set (`ConversationKeyService`, column `email_thread.conversation_key`, V32). Several email threads can merge into one chat timeline.

## Auth model
Session cookie (`JSESSIONID`) + persistent remember-me (30 d); CSRF enforced except `/h2-console/**` and `/billing/webhook`. **No token/bearer auth exists.** Public routes are the `permitAll` list in `SecurityConfig`; everything else requires login.

## Web vs. REST
There is **no REST API**. All user-facing behaviour is form-POST + server-rendered HTML. Three JSON/stream endpoints exist (see `API.md`). A mobile API (`/api/v1`) is *planned*; it must call the same services the controllers use (`ChatService`, `ConversationListService`, `ReplyService`, `OutboundMessageService`, `MailAccountService`). Caveat: `ConversationController` currently holds orchestration logic inline (recipient parsing, sender selection, signature append, key computation) — this must be extracted into a service before a second caller exists (see INTERNAL_TODO).

## Quality gates
`./mvnw -q -B verify` (tests: JUnit 5, H2; Testcontainers/GreenMail integration tests need Docker — one is skipped without it).
