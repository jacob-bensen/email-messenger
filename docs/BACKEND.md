# Backend architecture

Repo classification: **BACKEND** (pom.xml + Spring Boot + Flyway `db/migration`, Thymeleaf templates).
Sibling: `conexus-mail-mobile` (Flutter client; currently an empty scaffold).

## Stack (as implemented — differs from the generic target)
- Java 21, Spring Boot 3, Maven wrapper (`./mvnw`).
- Persistence is **Spring Data JPA / Hibernate** (`ddl-auto: validate`), NOT jOOQ. There is no jOOQ codegen.
- PostgreSQL 16 (prod/local), H2 (tests). Flyway migrations `V1..V35` in `src/main/resources/db/migration` (immutable once merged).
- Thymeleaf server-rendered UI; Spring Security (form login + Google OIDC + remember-me, CSRF on).

## Package layout (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats, mailboxes, saved searches, PWA, SEO, legal), `ChatService`, `ConversationListService`, SSE `ChatEventStream` |
| `service` | Email → IM transform (`IMTransformService`, `EmailBodyCleaner`), `ConversationService`, `ReplyService` (SMTP send), view records (`ChatConversation`, `BubbleRun`, …) |
| `email` | Import/polling: `EmailImportService`, `MimeMessageParser`, `MailboxPollingService`, IMAP/IDLE clients, Gmail OAuth, credential encryption |
| `auth` | Security config, registration/login/reset/verify, account settings, Google OIDC |
| `billing` | Stripe checkout/portal/webhook, `PlanLimits` (FREE 3 mailboxes/500 threads; PRO 5/unlimited; BUSINESS unlimited), trial nudges |
| `team` | Teams + invites |
| `digest`, `activation`, `reengagement`, `admin` | Scheduled emails and admin metrics |
| `domain`, `repository` | JPA entities and Spring Data repos |

## Domain / schema overview
Tables: `users`, `persistent_logins`, `subscriptions`, `plan_change_events`, `mail_accounts` (IMAP password or Gmail OAuth; encrypted creds; per-account last-seen UIDs; signature), `participants`, `email_threads` (owner, unread, `conversation_key`), `messages` (outbound flag), `message_recipients`, `attachments` (`blob_ref` always null — bytes not stored), `saved_searches`, `digest_email_preferences`, `password_reset_tokens`, `email_verifications`, `auth_events`, `teams`, `team_members`, `team_invites`.

A **conversation** (`/chats/{key}`) = all messages exchanged with one participant set (`conversationKey`), merged across email threads, rendered as bubble runs.

## Auth model
Session cookie (`JSESSIONID`) via `POST /login` (`email`, `password`, optional `remember-me`) plus CSRF token on every state-changing request; Google OIDC at `/auth/google/start`. **No token / API-key auth exists.** Everything except the public list in `SecurityConfig` requires authentication. `/billing/webhook` is CSRF-exempt (Stripe signature).

## Web vs REST
There is **no REST API yet**. The only JSON/SSE endpoints are `POST /chats/refresh`, `GET /chats/events` (SSE) and `POST /billing/webhook`. All business logic lives in services (`ChatService`, `ConversationListService`, `ReplyService`, `OutboundMessageService`, `MailAccountService`, …) so a future `/api/v1` controller layer can reuse them without forking logic. Note: some orchestration (recipient parsing, account selection, signature appending, `selectMailbox`) lives in `ConversationController` and must move to a service before an API reuses it (see INTERNAL_TODO).
