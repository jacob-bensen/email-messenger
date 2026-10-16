# BACKEND.md — architecture

Reverse-engineered from the code on 2026-10-16. Anything not verified is marked TBD.

## Stack (actual)
Java 21, Spring Boot 3, **Spring Data JPA/Hibernate** (`ddl-auto: validate`; **not jOOQ** — there is no codegen),
PostgreSQL 16 (H2 in tests), Flyway (`src/main/resources/db/migration`, V1..V35), Thymeleaf, Spring Security
(form login + optional Google OIDC + persistent remember-me), Stripe (`stripe-java`), Jakarta Mail (IMAP/SMTP), jsoup.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `domain` | JPA entities: `User`, `Subscription`, `Plan`, `MailAccount`, `EmailThread`, `Message`, `MessageRecipient`, `Participant`, `Attachment`, `SavedSearch`, `Team`/`TeamMember`/`TeamInvite`, `AuthEvent`, token entities, `PlanChangeEvent` |
| `repository` | Spring Data repositories (one per aggregate) |
| `email` | Mailbox integration: `MimeMessageParser`, `EmailImportService`, `ImapClient`/`JakartaImapClient`, `ImapIdleService` (paid tiers), `MailboxPollingService`/`Scheduler`, `GmailOAuthClient`, `MailAccountService`, `CredentialEncryptor` |
| `service` | Conversation logic: `ConversationKeyService` (participant-set key), `IMTransformService`, `EmailBodyCleaner`, `ChatConversation`/`BubbleRun` view models, `ReplyService` |
| `web` | Thymeleaf controllers + `ChatService`, `ConversationListService`, `OutboundMessageService`, `ChatEventStream` (SSE), PWA/SEO/legal controllers |
| `auth` | `SecurityConfig`, registration, login throttle, email verification, password reset, Google OIDC, account settings |
| `billing` | Checkout/portal/cancel, Stripe webhook, `PlanLimits` (Free 3 mailboxes/500 threads; Pro 5/unlimited; Business unlimited) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, weekly digests, lifecycle emails, revenue/funnel metrics |

## Auth model
Session cookie (`JSESSIONID`) + CSRF for all POSTs (only `/billing/webhook` and `/h2-console/**` exempt).
Everything not in the `permitAll` list in `SecurityConfig` requires authentication; unauthenticated requests
redirect to `/login` (HTML). **There is no token/Bearer auth and no JSON error model** — see INTERNAL_TODO.

## Web vs REST
The app is entirely server-rendered. The only JSON/stream endpoints are `POST /chats/refresh`, `GET /chats/events`
(SSE) and the Stripe webhook. **There is no REST API for a mobile client yet** (see `API.md`).

## Business rules relevant to a client
- Conversations keyed by participant-set `conversationKey` (V32), scoped per mailbox address or "all".
- Plan caps in `PlanLimits`; enforced via `PlanLimitService` (`PlanLimitExceededException`).
- Attachments: metadata only; `blob_ref` always null, not downloadable (roadmap).
- Outbound send via SMTP (`OutboundMessageService`, `ReplyService`); delivery states not implemented (roadmap).
