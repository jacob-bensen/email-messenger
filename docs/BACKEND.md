# Backend architecture

Reverse-engineered from the code on 2026-10-09. Unknowns are marked TBD.

## Stack (actual)
Java 21, Spring Boot 3, Spring Data **JPA/Hibernate** (not jOOQ — there is no
codegen step), PostgreSQL 16 (H2 in tests), Flyway (`V1`–`V35`), Thymeleaf,
Spring Security (session cookie + CSRF), Stripe, Jakarta Mail (IMAP/SMTP),
Gmail OAuth. Maven wrapper. Package root `com.emailmessenger`.

## Modules (`src/main/java/com/emailmessenger/`)
| Package | Role |
|---|---|
| `auth` | Form login, Google OIDC, registration, email verification, password reset, account settings, login throttling, `SecurityConfig` |
| `billing` | Free/Pro/Business plans, Stripe checkout/portal/webhook, `PlanLimits` caps (mailboxes / threads / saved searches) |
| `email` | IMAP client, polling + IDLE (`ImapIdleService`), MIME parsing, import (`EmailImportService`), Gmail OAuth, credential encryption |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), conversation keys, `ReplyService`, view models (`ChatConversation`, `BubbleRun`, …) |
| `web` | Thymeleaf controllers, `ChatService`, `ChatEventStream` (SSE), `OutboundMessageService`, PWA/SEO endpoints |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `team`, `digest`, `admin`, `activation`, `reengagement` | Teams + invites, digests, admin metrics, lifecycle emails |

## Data model (overview)
`User` → `MailAccount` (IMAP password or Gmail OAuth; encrypted creds) →
`EmailThread` → `Message` (+ `MessageRecipient`, `Attachment`), `Participant`
(deduped by normalized address). Conversations in `/chats` are keyed by
`email_thread.conversation_key` (participant set, V32), not by thread id.
Also: `Subscription`, `PlanChangeEvent`, `SavedSearch`, `Team*`, `AuthEvent`,
token tables. `Attachment.blob_ref` is always null (bytes are not stored).

## Auth model
Browser session cookie (`JSESSIONID`) + persistent remember-me (30 d), CSRF on
for everything except `/billing/webhook` and `/h2-console/**`. Login is form
POST `/login` (`email`, `password`) or Google OIDC. **There is no token
(Bearer/JWT) auth and no `/api/**` surface**; a mobile client cannot currently
authenticate except by emulating a browser.

## Web vs. API
The app is purely server-rendered. The only non-HTML responses are
`POST /chats/refresh` (JSON), `GET /chats/events` (SSE), the Stripe webhook,
and PWA/SEO assets. See `API.md`/`WEB.md`. Controllers call services
(`ChatService`, `MailAccountService`, `OutboundMessageService`, …); some
controller-local helpers (`selectMailbox`, `scopeAddress`, `parseRecipients`
in `ConversationController`) hold logic that a REST controller would need
too — see INTERNAL_TODO.
