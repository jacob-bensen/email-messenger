# Backend architecture (ConexusMail)

Reverse-engineered 2026-10-05 from the code. Unknowns are marked TBD.

## Stack
Java 21, Spring Boot 3, **Spring Data JPA** (NOT jOOQ — the generic brief assumed jOOQ; there is no codegen step),
PostgreSQL (H2 in tests), Flyway (`src/main/resources/db/migration`, V1..V35), Thymeleaf, Maven wrapper.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`ConversationController` = /chats workspace, `MailboxController`, `GmailConnectController`, `SavedSearchController`, `PricingController`, `LegalController`, `PwaController`, `SeoController`, `ChatEventsController` SSE), `ChatService`, `ConversationListService`, `OutboundMessageService` |
| `service` | View models/records (`ChatConversation`, `ConversationListItem`, `BubbleRun`…), `IMTransformService`, `EmailBodyCleaner`, `ReplyService`, `ConversationKeyService` |
| `email` | IMAP/Gmail import: `MailboxPollingService`, `ImapIdleService`, `MimeMessageParser`, `EmailImportService`, `MailAccountService`, `GmailOAuthClient`, `CredentialEncryptor` |
| `domain` / `repository` | JPA entities / Spring Data repos (User, MailAccount, EmailThread, Message, Participant, Attachment, Subscription, Team…) |
| `auth` | Form login + Google OIDC, password reset, email verification, `SecurityConfig` |
| `billing` | Stripe checkout/portal/webhook, plans FREE/PRO/BUSINESS |
| `team`, `digest`, `activation`, `reengagement`, `admin` | teams/invites, digests, lifecycle emails, admin metrics |

## Core model
`User` → many `MailAccount` (IMAP password or Gmail OAuth; credentials encrypted) → imported `EmailThread`/`Message`/`Participant`.
Threads are grouped into **conversations** by `conversationKey` (hash of participant set, V32). The chat view merges all
threads sharing a key into one timeline of bubble runs. Outbound messages are flagged (V30).

## Auth model
Session cookie (`JSESSIONID`) + remember-me (JDBC token repo, 30 days), CSRF **enabled** (ignored only for `/h2-console/**`,
`/billing/webhook`). Everything except a public allow-list requires authentication. **There is no token/Bearer auth** —
see INTERNAL_TODO (mobile blocker).

## Web vs REST
All HTTP surface today is server-rendered web. Only JSON-ish endpoints: `POST /chats/refresh` (`{"imported":n}`) and the
SSE stream `GET /chats/events`. No `/api/**` exists. Planned mobile API: see `docs/API.md` §Planned.
