# BACKEND.md — architecture

Repo classification: BACKEND (pom.xml, Spring Boot 3.5, Flyway migrations V1–V35). Sibling: `conexus-mail-mobile` (Flutter, currently only a README).

## Stack (actual)
Java 21, Spring Boot 3.5.14, Spring Data **JPA** (not jOOQ — no codegen step), PostgreSQL 16 / H2 in tests, Flyway, Thymeleaf, Jakarta Mail (IMAP/SMTP), Stripe, Maven wrapper.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats, mailboxes, pricing, PWA, SEO), `ChatService`, `ConversationListService`, SSE `ChatEventStream` |
| `service` | Conversation/IM transform (`IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`), `ReplyService` |
| `email` | IMAP/SMTP import, polling, IDLE, Gmail OAuth, credential encryption |
| `auth` | Security config, registration, login throttle, password reset, email verification, Google OIDC, account pages |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digests, lifecycle emails, admin metrics |
| `domain`, `repository` | JPA entities / Spring Data repos |

## Schema overview
Tables: users, subscriptions, plan_change_events, mail_accounts, email_threads, messages, message_recipients, participants, attachments (`blob_ref` always null today), saved_searches, thread_notes, teams, team_members, team_invites, digest_email_preferences, auth_events, password_reset_tokens, email_verifications, persistent_logins. Conversations are keyed by participant-set `conversationKey` (not stored as a table).

## Auth model
Session cookie (`JSESSIONID`) via form login (`POST /login`, params `email`, `password`, optional `remember-me` 30d) or Google OIDC. **CSRF is enabled** (only `/h2-console/**` and `/billing/webhook` exempt). Everything outside the permit-list in `SecurityConfig` requires authentication. No token/bearer auth exists.

## Web vs REST
There is **no REST API**. All controllers return Thymeleaf views/redirects, except: `POST /chats/refresh` (JSON), `GET /chats/events` (SSE), `POST /billing/webhook` (Stripe). See `API.md`. Business logic lives in services, shared by controllers.
