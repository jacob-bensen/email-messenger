# Backend architecture

Reverse-engineered 2026-10-17 from the code. Anything not verified is marked TBD.

## Stack
Java 21, Spring Boot 3, Spring Data **JPA** (not jOOQ — no codegen step), Flyway
(`src/main/resources/db/migration`, V1..V35, immutable), PostgreSQL 16 (H2 in tests),
Thymeleaf, Spring Security, Stripe (stripe-java), Jakarta Mail (IMAP/SMTP), jsoup.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, landing/legal/PWA/SEO), `ChatEventStream` (SSE), `ChatService`, `ConversationListService`, `OutboundMessageService` |
| `auth` | Form login + Google OIDC, registration, password reset, email verification, account pages, login throttling, remember-me |
| `billing` | Stripe checkout/portal/webhook, `PlanLimits` (FREE/PRO/BUSINESS caps), trial nudges |
| `email` | IMAP polling (`MailboxPollingService`), IDLE (`ImapIdleService`), MIME parsing, Gmail OAuth, credential encryption |
| `service` | Conversation building: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ReplyService` (SMTP), `ConversationKeyService` |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams + invites, weekly digest, lifecycle emails, admin metrics |

## Domain
`User`, `Subscription`/`Plan`, `MailAccount` (IMAP password or Gmail OAuth, per-account signature,
polling policy, last-seen UID cursors), `EmailThread` (owned by user, has `conversationKey`),
`Message` (+`MessageRecipient`, `Attachment`; `outbound` flag), `Participant` (deduped by email),
`SavedSearch`, `Team`/`TeamMember`/`TeamInvite`, token tables, `AuthEvent`, `PlanChangeEvent`.

A **conversation** = all messages sharing a participant set (`conversationKey`), merged across
email threads; rendered as `ChatConversation` (members, bubble runs). The list is `ConversationListItem`.

## Auth model
Session cookie (`JSESSIONID`) + remember-me (30 d), **CSRF enabled** on all POSTs except
`/billing/webhook`. Form login: `POST /login` (params `email`, `password`). Google OIDC only if
`GOOGLE_CLIENT_ID` is set. Everything is authenticated except the public list in `SecurityConfig`.
**There is no token/bearer auth — a mobile client cannot authenticate today** (see INTERNAL_TODO).

## Web vs API
No REST API exists. The only JSON/non-HTML endpoints: `POST /chats/refresh` (`{"imported":n}`),
`GET /chats/events` (SSE), `/billing/webhook`, PWA/SEO assets. See WEB.md and API.md.
