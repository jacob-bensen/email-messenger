# BACKEND.md — architecture

Reverse-engineered 2026-11-03 from the code. Unknowns are marked TBD.

## Stack
Java 21, Spring Boot 3, Spring Data **JPA/Hibernate** (`ddl-auto: validate`), Flyway
(V1–V34 in `src/main/resources/db/migration`), PostgreSQL (H2 in tests), Thymeleaf,
Spring Security, Stripe, Jakarta Mail (IMAP/SMTP), GreenMail + Testcontainers in tests.
**There is no jOOQ.** Persistence is JPA entities + Spring Data repositories.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `domain` | JPA entities: User, MailAccount, EmailThread, Message, MessageRecipient, Participant, Attachment, Subscription, Plan, Team*, SavedSearch, AuthEvent, tokens |
| `repository` | Spring Data repositories |
| `email` | IMAP import/polling/IDLE, MIME parsing, Gmail OAuth, credential encryption |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), `ConversationService`, `ReplyService` (SMTP send), `ConversationKeyService`, view records (`ChatConversation`, `BubbleRun`, ...) |
| `web` | Thymeleaf controllers, `ChatService`, `ConversationListService`, `OutboundMessageService`, SSE (`ChatEventStream`) |
| `auth` | Security config, form login + Google OIDC, registration, password reset, email verify |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | teams/invites, digests, lifecycle emails, admin metrics |

## Core model
Conversations are keyed by `conversationKey` = participant set (`ConversationKeyService`),
spanning one or more `EmailThread`s. UI shows a merged timeline of bubble runs.
Plans: FREE / PRO / BUSINESS; limits enforced by `PlanLimitService` (`PlanLimitKind`).

## Auth model
Session cookie (`JSESSIONID`) + persistent remember-me, **CSRF on** (exempt: `/billing/webhook`).
Form login (`/login`, params `email`,`password`) and Google OIDC. Everything not in the
permitAll list in `SecurityConfig` requires authentication. **No token/bearer auth exists.**

## Web vs REST
Only HTML (Thymeleaf) controllers exist, plus tiny `@ResponseBody` helpers
(`POST /chats/refresh`, SSE `/chats/events`, Stripe webhook). **There is no REST API for a mobile
client yet.** See `API.md` (proposed contract) and `INTERNAL_TODO.md`.
Business logic leaks into `ConversationController` (sender-account selection, signature
append, conversation-key computation, send orchestration) — must move to a service before a
REST layer can share it.
