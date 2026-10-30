# BACKEND.md — architecture

Reverse-engineered from the code on 2026-10-30. Unknowns are marked TBD.

## Stack
Java 21, Spring Boot 3, Spring Data **JPA** (not jOOQ — the routine's jOOQ assumption does not hold here), PostgreSQL 16 (H2 in tests), Flyway (`src/main/resources/db/migration`, V1–V35), Thymeleaf, Jakarta Mail (IMAP/SMTP), Stripe. Maven wrapper.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`ConversationController` = `/chats`, `MailboxController`, `GmailConnectController`, `SavedSearchController`, `PwaController`, `SeoController`, `LegalController`, `PricingController`), SSE (`ChatEventStream`/`ChatEventsController`), model advice, `ChatService`, `ConversationListService`, `OutboundMessageService`, `OnboardingService` |
| `service` | Conversation building and IM transform: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`; view records (`ChatConversation`, `BubbleRun`, ...) |
| `email` | Mailbox integration: `ImapClient`, `MailboxPollingService/Scheduler`, `ImapIdleService`, `EmailImportService`, `MimeMessageParser`, `GmailOAuthClient`, `CredentialEncryptor`, `MailAccountService` |
| `auth` | Security (`SecurityConfig`), form login + Google OIDC, registration, password reset, email verification, account settings, login throttling |
| `billing` | Stripe checkout/portal/webhook, plans (`Plan`: FREE/PRO/BUSINESS), `PlanLimits`, `PlanLimitService` |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digests, lifecycle emails (schedulers, off unless `*.enabled`), operator metrics (`/admin/**`, allowlist `admin.emails`) |
| `domain`, `repository` | JPA entities and Spring Data repositories |

## Core model
`User` → `MailAccount`(s) (IMAP password or Gmail OAuth, credentials encrypted) → `EmailThread` (owner, `conversation_key` = participant set) → `Message` (+ `MessageRecipient`, `Attachment`; `blob_ref` always null — bytes not stored) ↔ `Participant`. Also `Subscription`, `PlanChangeEvent`, `Team*`, `SavedSearch`, `DigestEmailPreference`, `AuthEvent`, token tables, `persistent_logins`.
A "chat" = all messages sharing one `conversationKey`, merged across threads.

## Auth model
Session cookie (`JSESSIONID`) + remember-me (30 d, persistent_logins). Form login at `POST /login` (`email`, `password`), optional Google OIDC (`/auth/google/start`). **CSRF is on** except `/h2-console/**` and `/billing/webhook`. Unauthenticated access is limited to the permit-list in `SecurityConfig`. There is **no token (bearer/JWT) auth**; a mobile client cannot authenticate today.

## Web vs API
Today the app is server-rendered only. The sole JSON endpoints are `POST /chats/refresh` and `POST /billing/webhook`; plus an SSE stream. See `API.md` and `WEB.md`.
