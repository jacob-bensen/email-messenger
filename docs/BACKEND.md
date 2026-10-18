# BACKEND.md — architecture

Reverse-engineered from the code on 2026-10-18. Anything not implemented is marked TBD.

## Stack (actual)
Java 21, Spring Boot 3, Spring Data **JPA** (not jOOQ — no jOOQ codegen exists), Flyway (35 migrations, V1–V35),
PostgreSQL (prod/local) / H2 (tests), Thymeleaf, Spring Security, Stripe, Jakarta Mail (IMAP/SMTP), Maven wrapper.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats, mailboxes, pricing, legal, PWA, SEO), SSE `ChatEventStream`, `ChatService`, `ConversationListService` |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), `ConversationService`, `ConversationKeyService`, `ReplyService`; view-model records |
| `email` | IMAP import/polling/IDLE, Gmail OAuth, MIME parsing, credential encryption |
| `auth` | Security config, registration, login throttling, password reset, email verification, Google OIDC, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | teams/invites, weekly digest, lifecycle emails, operator metrics (`/admin/revenue`) |
| `domain`, `repository` | JPA entities / Spring Data repositories |

## Data model (tables)
users, persistent_logins, subscriptions, plan_change_events, mail_accounts, email_threads (has `conversation_key`, owner),
messages (has outbound flag), message_recipients, participants, attachments (`blob_ref` always null — bytes not stored),
saved_searches, digest_email_preferences, teams/team_members/team_invites, password_reset_tokens, email_verifications, auth_events.

## Auth model
Session cookie (JSESSIONID) + remember-me (30d, DB token repo). Form login at `POST /login` (params `email`, `password`)
plus optional Google OIDC (`/auth/google/start`). **CSRF is on** except `/billing/webhook` and `/h2-console/**`.
Everything outside the public allow-list in `SecurityConfig` requires authentication. **There is no token/bearer auth.**

## Web vs API
The app is entirely server-rendered. The only non-HTML responses are SSE `/chats/events`, PWA/SEO assets and the Stripe
webhook. **No REST/JSON API for a mobile client exists yet** — see `API.md`. Business logic already lives in services
(`ChatService`, `ReplyService`, `MailAccountService`, `PlanLimitService`, `BillingService`), so a future `/api/v1` layer
must call those, not controllers.
