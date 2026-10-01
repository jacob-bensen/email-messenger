# Backend architecture

Classification: BACKEND (pom.xml + Spring Boot + Flyway, no pubspec). Sibling: `conexus-mail-mobile` (Flutter client, currently only a README).

## Stack (as implemented)
Java 21, Spring Boot 3, **Spring Data JPA / Hibernate** (`ddl-auto: validate`; there is **no jOOQ** in `pom.xml`), PostgreSQL 16 (H2 in tests), Flyway (`src/main/resources/db/migration`, V1..V35), Thymeleaf, Spring Security, Stripe, Jakarta Mail (IMAP/SMTP), Maven wrapper.

## Package layout (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`ConversationController` = /chats workspace, mailboxes, saved searches, pricing/landing, PWA, SEO, SSE `ChatEventStream`), model advices, `ChatService`, `ConversationListService`, `OutboundMessageService` |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), `ConversationService`, `ConversationKeyService`, `ReplyService`, bubble view records |
| `email` | IMAP/SMTP + Gmail OAuth, `EmailImportService`, `MailboxPollingService`, `ImapIdleService`, credential encryption |
| `auth` | Security config, form login + Google OIDC, registration, verification, password reset, account settings, login throttling |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digests, lifecycle emails, admin metrics |
| `domain`, `repository` | JPA entities, Spring Data repositories |

## Data model overview
`User`, `Subscription`/`Plan`, `MailAccount` (IMAP password or Gmail OAuth, encrypted creds), `EmailThread`, `Message` (message_id_header, in_reply_to, body_plain/html, sent_at, outbound), `MessageRecipient`, `Participant` (deduped by normalized address), `Attachment` (`blob_ref` always null today), `SavedSearch`, `Team*`, token/audit tables. Conversations in the UI are keyed by participant-set `conversationKey`.

## Auth model
Session cookie (form login `POST /login`, params `email`/`password`; optional Google OIDC; remember-me 30d). **CSRF enabled**, ignored only for `/h2-console/**` and `/billing/webhook`. Everything not on the permit-list in `SecurityConfig` requires authentication. There is no token/API-key auth and no `/api/**` namespace.

## Web vs API
All HTTP surface today is server-rendered (see `WEB.md`). Controllers resolve the user from `Principal` and call services; no REST controllers exist except `POST /billing/webhook` (Stripe) and JSON-ish `POST /chats/refresh` / SSE `/chats/events`. Mobile client has **no supported API yet** — see `API.md` and `INTERNAL_TODO.md`.
