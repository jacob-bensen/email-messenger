# BACKEND.md — architecture

Repo classification: **BACKEND** (pom.xml + Spring Boot + Flyway `db/migration`). Sibling: `conexus-mail-mobile` (FRONTEND, Flutter; currently README only).

## Stack (as implemented)
Java 21, Spring Boot 3.5.x, Spring Data **JPA** (not jOOQ — no codegen step), PostgreSQL 16 (H2 in tests), Flyway, Thymeleaf, Spring Security, Jakarta Mail (IMAP/SMTP), Stripe. Maven wrapper.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats, mailboxes, saved searches, landing/pricing/legal, PWA, SEO), SSE stream, view services (`ChatService`, `ConversationListService`, `OutboundMessageService`, `OnboardingService`) |
| `service` | Conversation logic: `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService` (SMTP send), bubble/run view records |
| `email` | IMAP import (`EmailImportService`, `MimeMessageParser`), polling (`MailboxPollingService/Scheduler`), IMAP IDLE, Gmail OAuth, credential encryption |
| `domain` / `repository` | JPA entities / Spring Data repositories |
| `auth` | Security config, form login, Google OIDC, registration, email verification, password reset, account settings, login throttling, auth audit events |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimits`: Free 3 mailboxes/500 threads, Pro 5/unlimited, Business unlimited), trial nudges |
| `team` | Teams and invites |
| `digest`, `reengagement`, `activation`, `admin` | Email digests, lifecycle emails, admin revenue/metrics |

## Schema overview (Flyway V1–V35, immutable once merged)
Tables: `users`, `subscriptions`, `plan_change_events`, `mail_accounts`, `email_threads` (owner + `conversation_key`), `messages` (+`outbound` flag), `message_recipients`, `participants`, `attachments` (`blob_ref` always null today), `saved_searches`, `teams`, `team_members`, `team_invites`, `digest_email_preferences`, `auth_events`, `password_reset_tokens`, `email_verifications`, `persistent_logins`. (`thread_notes` dropped in V33.)

## Auth model
Session-cookie based (`JSESSIONID`), form login at `POST /login` (`email`, `password`, optional `remember-me` 30d), Google OIDC when configured, CSRF **enabled** (exempt: `/h2-console/**`, `/billing/webhook`). Login throttling filter. Everything not on the `permitAll` list in `SecurityConfig` requires authentication. **There is no token/bearer auth yet** — see `API.md` and `INTERNAL_TODO.md`.

## Web vs REST
The only server surface today is server-rendered Thymeleaf (`WEB.md`) plus three non-HTML endpoints (`/chats/refresh` JSON, `/chats/events` SSE, `/billing/webhook`). No `/api/**` REST layer exists for the mobile client.
