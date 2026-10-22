# BACKEND.md — architecture

Repo classification: **BACKEND** (pom.xml, Spring Boot, Flyway migrations V1–V35).
Sibling: `conexus-mail-mobile` (FRONTEND, Flutter; currently an empty scaffold).

## Stack (as implemented)
Java 21, Spring Boot 3.5, Spring Data **JPA** (not jOOQ — the target stack in the
sync routine assumed jOOQ; this repo never used it), Flyway, PostgreSQL 16 (H2 in
tests), Thymeleaf, Spring Security (form login + Google OIDC + remember-me), Stripe,
Jakarta Mail (IMAP/SMTP), jsoup.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats, mailboxes, saved searches, PWA, SEO, legal, pricing), `ChatService`, `ConversationListService`, `OutboundMessageService`, SSE `ChatEventStream` |
| `auth` | Security config, registration/login, Google OIDC, password reset, email verification, account pages, login throttling, audit events |
| `service` | Mail → IM transform: `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`, view records (`ChatConversation`, `BubbleRun`, …) |
| `email` | IMAP/SMTP: import, polling (`MailboxPollingService/Scheduler`), IMAP IDLE, Gmail OAuth, credential encryption |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`), trial/banner logic |
| `team` | Teams and invites |
| `digest`, `activation`, `reengagement`, `admin` | Scheduled emails and admin metrics |
| `domain` / `repository` | JPA entities / Spring Data repositories |

## Data model (see `db/migration`)
`User`, `Subscription` (plan FREE/PRO/BUSINESS), `MailAccount` (IMAP password or
Gmail OAuth; credentials encrypted), `EmailThread` (owner-scoped, has
`conversation_key` = participant-set key), `Message` (inbound/outbound flag),
`MessageRecipient`, `Participant`, `Attachment` (`blob_ref` always null today),
`SavedSearch`, `Team*`, token/audit tables. Migrations are immutable; add new `V{n}`.

## Auth model
Session cookie (`JSESSIONID`) or 30-day persistent remember-me. CSRF is ON for all
POSTs except `/billing/webhook` and `/h2-console/**`. Everything not in the permit-list
in `auth/SecurityConfig` requires authentication. **There is no token/bearer auth,
so a mobile client cannot authenticate today** (see INTERNAL_TODO).

## Web vs REST
All HTTP endpoints are server-rendered MVC. The only JSON endpoint is
`POST /chats/refresh`; the only stream is SSE `GET /chats/events`. Controllers delegate
to services (`ChatService`, `ConversationListService`, `OutboundMessageService`,
`MailboxPollingService`, `MailAccountService`), so a future `/api/v1` layer should
reuse those services, not the controllers.

## Commands
`./mvnw -B test` (needs network for first dependency download).
