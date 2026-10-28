# Backend architecture

Spring Boot 3 / Java 21 monolith ("ConexusMail"). Persistence is **Spring Data JPA** (not jOOQ) on PostgreSQL 16 (H2 in tests), schema managed by **Flyway** (`src/main/resources/db/migration`, V1..V35, immutable once merged).

## Modules (`com.emailmessenger.*`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, pricing/landing/legal/PWA/SEO), `ChatEventStream` (SSE) |
| `auth` | Form login + Google OIDC, registration, password reset, email verification, account settings, login throttle |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), banners, trial nudges |
| `email` | IMAP/Gmail import (`EmailImportService`, `MimeMessageParser`), polling, IMAP IDLE, credential encryption |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), `ConversationService`, `ReplyService`, conversation keys |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `team`, `digest`, `admin`, `activation`, `reengagement` | Teams/invites, digests, admin metrics, lifecycle emails |

## Data model (tables)
users, subscriptions, plan_change_events, mail_accounts, participants, email_threads, messages, message_recipients, attachments (`blob_ref` always null today), saved_searches, thread_notes, teams, team_members, team_invites, digest_email_preferences, password_reset_tokens, email_verifications, auth_events, persistent_logins.
Conversations are keyed by a participant-set `conversationKey`.

## Auth model
Session cookie (`JSESSIONID`) via form login (`/login`, params `email`/`password`) or Google OIDC; remember-me (30 days, persistent token table). **CSRF is enabled** (exempt: `/billing/webhook`, `/h2-console/**`). Everything not in the public allow-list in `auth/SecurityConfig` requires authentication. There is **no token (JWT/API-key) auth** yet.

## Web vs. API
All HTTP surface today is server-rendered web. Controllers resolve the user via `UserService.requireByEmail(principal)` and delegate to services (`ConversationListService`, `ChatService`, `OutboundMessageService`, `MailAccountService`). Only JSON-ish endpoints: `POST /chats/refresh` (`{"imported":n}`), SSE `GET /chats/events`, Stripe webhook. A mobile REST API must be added as thin controllers over the **same services** — see `docs/API.md`.
