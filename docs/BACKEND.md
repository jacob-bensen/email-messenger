# Backend architecture (ConexusMail / email-messenger)

Reverse-engineered from the code on 2026-11-09. Unknowns are marked TBD.

## Stack (actual)
Java 21, Spring Boot 3, **Spring Data JPA/Hibernate** (not jOOQ), Flyway, PostgreSQL 16 (H2 in tests),
Thymeleaf, Spring Security, Stripe, Jakarta Mail (IMAP/SMTP), Maven wrapper (`./mvnw`).

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats, mailboxes, saved searches, pricing, legal, PWA, SEO), SSE `ChatEventsController`, `GlobalExceptionHandler` |
| `auth` | Form login + Google OIDC, registration, password reset, email verification, account settings, login throttle, `SecurityConfig` |
| `billing` | Stripe checkout/portal/webhook, plans & limits (`PlanLimitService`), trial nudges |
| `email` | IMAP import/polling/IDLE, Gmail OAuth, SMTP-side helpers, credential encryption |
| `service` | Conversation building: `ChatService`, `ConversationKeyService`, `IMTransformService`, `EmailBodyCleaner`, `ReplyService` |
| `domain` / `repository` | JPA entities / Spring Data repositories |
| `team`, `digest`, `activation`, `reengagement`, `admin` | teams & invites, weekly digests, lifecycle emails, operator metrics |

## Schema overview (Flyway `V1`..`V35`, immutable once merged)
Tables: `users`, `persistent_logins`, `subscriptions`, `plan_change_events`, `mail_accounts`,
`participants`, `email_threads` (has `conversation_key`), `messages` (outbound flag), `message_recipients`,
`attachments` (`blob_ref` always null today), `saved_searches`, `digest_email_preferences`,
`password_reset_tokens`, `email_verifications`, `auth_events`, `teams`, `team_members`, `team_invites`.
A *conversation* = all threads sharing a participant-set `conversationKey`.

## Auth model
Server-side **session cookie** (JSESSIONID) + persistent remember-me (30 days, DB-backed), CSRF enabled
(exempt: `/h2-console/**`, `/billing/webhook`). Form login at `POST /login` (`email`, `password`);
optional Google OIDC (only if `GOOGLE_CLIENT_ID` set). Everything not whitelisted in `SecurityConfig` requires auth.
**There is no token (Bearer/JWT) auth and no `/api/**` namespace.**

## Web vs. REST
The server exposes no REST API for a mobile client. Existing controllers return Thymeleaf views/redirects, except
`POST /chats/refresh` (JSON `{imported:int}`), `GET /chats/events` (SSE) and `POST /billing/webhook`.
Controllers delegate to services (`ChatService`, `MailAccountService`, `OutboundMessageService`, ...), so a future
`/api/v1` layer should call the same services. See `docs/API.md` and `INTERNAL_TODO.md`.

## Operations
Schedulers: mailbox polling, IMAP IDLE (paid tiers), digests, activation/re-engagement/trial emails.
Deploy notes: `DEPLOY.md`. Owner-facing planning docs: `master/` (PLAN, BACKLOG, CHANGELOG, MASTER_ACTIONS).
