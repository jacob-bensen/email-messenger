# BACKEND architecture (email-messenger / "ConexusMail")

Reverse-engineered from the code on 2026-10-16. Anything not verified is marked TBD.

## Stack
Java 21, Spring Boot 3, **Spring Data JPA (Hibernate)** — *not* jOOQ — PostgreSQL 16
(H2 in tests), Flyway (`src/main/resources/db/migration`, V1–V35), Thymeleaf, Spring
Security (form login + optional Google OIDC), Stripe (`stripe-java`), Jakarta Mail (IMAP/SMTP),
GreenMail + Testcontainers in tests. Maven wrapper (`./mvnw`). There is no jOOQ codegen step.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `domain` | JPA entities: User, Subscription, Plan (FREE/PRO/BUSINESS), MailAccount, EmailThread, Message, MessageRecipient, Participant, Attachment, SavedSearch, Team/TeamMember/TeamInvite, tokens, AuthEvent, PlanChangeEvent |
| `repository` | Spring Data repositories |
| `email` | IMAP import (`EmailImportService`, `MimeMessageParser`), polling (`MailboxPollingService/Scheduler`), IMAP IDLE (`ImapIdleService`), Gmail OAuth, credential encryption |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), `ConversationService` (builds `ChatConversation`), `ReplyService` (SMTP send), `ConversationKeyService` |
| `web` | Thymeleaf controllers + view services (`ChatService`, `ConversationListService`, `OutboundMessageService`, SSE `ChatEventStream`) |
| `auth` | Security config, registration/login/reset/verify, Google OIDC, account pages |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimits`: FREE 3 mailboxes/500 threads, PRO 5/unlimited, BUSINESS unlimited) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | teams/invites, email digests, drip emails, operator revenue dashboard |

## Data model highlights
- A conversation is identified by `conversationKey` (V32): the participant set, merged across
  email threads. `/chats/{key}` is keyed by it.
- `Message.outbound` (V30) marks messages sent from the app. `blob_ref` on attachments is always
  null — attachment bytes are not stored (not downloadable).
- Mailbox credentials are encrypted with `CredentialEncryptor` (`MAILBOX_ENCRYPTION_*`).

## Auth model
Session cookie (`JSESSIONID`) + persistent remember-me (30 days, JDBC token repo), CSRF **on**
(exempt: `/h2-console/**`, `/billing/webhook`). Everything not on the permitAll list in
`auth/SecurityConfig` requires an authenticated session. **No token/bearer auth exists** —
a mobile client cannot authenticate today (see INTERNAL_TODO).

## Web vs. API
There is no REST API. All controllers are server-rendered Thymeleaf (`@Controller`), except:
`POST /chats/refresh` (JSON), `GET /chats/events` (SSE), `POST /billing/webhook`.
Send-flow orchestration (reply vs. new email, signature append, recipient derivation, outbound
recording) currently lives **inside `ConversationController.send/createMessage`**, not in a
service — it must be extracted before an API can share it (see INTERNAL_TODO).

## Config
Env vars: `JDBC_DATABASE_*`, `MAIL_*`, `GOOGLE_CLIENT_ID/SECRET`, `STRIPE_*`,
`MAILBOX_ENCRYPTION_*`, `MAILBOX_POLLING_ENABLED`, `MAILBOX_IDLE_ENABLED`, `ADMIN_EMAILS`
(see `application.yml`). Roadmap and product state: `CLAUDE.md`; operator ledger: `master/`.
