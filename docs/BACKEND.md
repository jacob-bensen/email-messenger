# Backend architecture

_Reverse-engineered 2026-10-04 from the code. Unknowns are marked TBD._

## Stack (actual)
Java 21, Spring Boot 3, **Spring Data JPA/Hibernate** (not jOOQ), PostgreSQL 16
(H2 in tests), Flyway (V1–V35 in `src/main/resources/db/migration`), Thymeleaf,
Spring Security (form login + Google OIDC, remember-me, CSRF on), Stripe, Jakarta
Mail (IMAP/SMTP), GreenMail + Testcontainers in tests. No Docker/CI by decision
(see CLAUDE.md).

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats workspace, mailboxes, pricing/landing, legal, PWA, SEO), SSE `ChatEventStream`, `ChatService`, `ConversationListService`, `OutboundMessageService` |
| `service` | Conversation view models (`ChatConversation`, `BubbleRun`…), `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService` (SMTP send) |
| `email` | IMAP import (`EmailImportService`, `MimeMessageParser`, `MailboxPollingService/Scheduler`, `ImapIdleService`), Gmail OAuth, credential encryption |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `auth` | Security config, registration, login throttle, password reset, email verification, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`) |
| `team`, `digest`, `admin`, `activation`, `reengagement` | Teams/invites, digests, admin metrics, lifecycle emails |

## Data model (overview)
`users` → `mail_accounts` (IMAP password or Gmail OAuth, polling cursors incl.
`last_seen_uid`, `last_seen_sent_uid`, signature) → `email_threads` (owner,
`conversation_key` = participant-set key, unread flag) → `messages`
(+ `message_recipients`, `attachments` with null `blob_ref`, `outbound` flag).
Also `participants`, `subscriptions`, `plan_change_events`, `saved_searches`,
teams/invites, `auth_events`, token tables, `persistent_logins` (remember-me).
Plans: `FREE`, `PRO`, `BUSINESS`.

## Auth model
Session cookie (JSESSIONID) + optional 30-day remember-me; CSRF token required on
all POSTs except `/billing/webhook` and `/h2-console/**`. Everything not in the
permit-list in `SecurityConfig` requires authentication. **No token/API-key auth
exists yet** — a mobile client cannot authenticate today (see INTERNAL_TODO).

## Web vs. API
There is **no REST API**. All UI is server-rendered. Business logic is mostly in
services, but `ConversationController.send` orchestrates `ReplyService` +
`OutboundMessageService` + signature appending inline — this must be extracted
into a service before a JSON API reuses it (INTERNAL_TODO #2).
