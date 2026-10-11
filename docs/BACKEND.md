# BACKEND.md — architecture

Reverse-engineered from the code on 2026-10-11. Anything not verified is marked TBD.

## Stack
Java 21, Spring Boot 3, Spring Data **JPA** (not jOOQ), Spring Security, Thymeleaf, Flyway, PostgreSQL (H2 in tests), Jakarta Mail (IMAP/SMTP), Stripe. Maven wrapper.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `auth` | Form login + Google OIDC, registration, password reset, email verification, login throttling, `SecurityConfig`, account pages |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimits`: Free 3 mailboxes/500 threads, Pro 5/unlimited, Business unlimited) |
| `domain` | JPA entities (User, Subscription, MailAccount, EmailThread, Message, Participant, Attachment, SavedSearch, Team…) |
| `repository` | Spring Data repositories |
| `email` | IMAP import/polling/IDLE, MIME parsing, Gmail OAuth, credential encryption |
| `service` | IM transform (quote stripping, bubble runs), conversation keys, `ReplyService` (SMTP) |
| `web` | Thymeleaf controllers, `ChatService`, `ConversationListService`, SSE (`ChatEventStream`), PWA/SEO |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digests, lifecycle emails, admin metrics |

## Schema
Flyway migrations `V1`..`V35` in `src/main/resources/db/migration` (immutable once merged). Tables: users, subscriptions, plan_change_events, mail_accounts, email_threads (keyed by `conversation_key`), messages, message_recipients, participants, attachments (`blob_ref` always null today), saved_searches, teams/team_members/team_invites, digest_email_preferences, auth_events, password_reset_tokens, email_verifications, persistent_logins.

## Auth model
Session cookie (`JSESSIONID`) + persistent remember-me; CSRF **enabled** (only `/h2-console/**`, `/billing/webhook` exempt). Everything except the public allow-list in `SecurityConfig` requires authentication; unauthenticated requests redirect to `/login` (HTML). **There is no token/bearer auth, so a native client cannot authenticate today** (see INTERNAL_TODO).

## Web vs REST
There is currently **no REST API**. The only JSON/stream endpoints are `POST /chats/refresh` and the SSE `GET /chats/events`. All other routes return Thymeleaf views. Controllers resolve the user via `UserService.requireByEmail(principal)` and delegate to services (`ChatService`, `ConversationListService`, `MailAccountService`, …). A future `/api/v1` layer MUST reuse these services rather than the controllers.
