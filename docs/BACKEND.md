# Backend architecture

Reverse-engineered from the code on 2026-10-24. Unknowns are marked TBD.

## Stack
Java 21, Spring Boot 3 (web, thymeleaf, data-jpa, validation, mail, security,
oauth2-client, actuator), Flyway, PostgreSQL (H2 in tests), Stripe, Jakarta
Mail (IMAP/SMTP), jsoup. **Persistence is JPA/Spring Data — not jOOQ**; there is
no codegen step.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, pricing/landing/PWA/SEO), `ChatEventStream` (SSE), chat/list services |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), `ConversationService`, `ConversationKeyService`, `ReplyService` |
| `email` | Mailbox integration: IMAP polling/IDLE, Gmail OAuth, `MimeMessageParser`, `EmailImportService`, `MailAccountService`, credential encryption |
| `auth` | Form login + Google OIDC, registration, email verification, password reset, account settings, login throttling/audit |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), trial nudges |
| `team` | Teams and invites |
| `digest`, `activation`, `reengagement` | Scheduled emails |
| `admin` | Admin revenue/retention metrics |
| `domain`, `repository` | JPA entities / Spring Data repositories |

## Schema overview
Flyway migrations `src/main/resources/db/migration/V1..V35` (immutable once
merged). Tables: `users`, `subscriptions`, `plan_change_events`, `mail_accounts`,
`email_threads`, `messages`, `message_recipients`, `participants`, `attachments`
(`blob_ref` always null — bytes are not stored), `thread_notes`, `saved_searches`,
`teams`, `team_members`, `team_invites`, `digest_email_preferences`,
`auth_events`, `email_verifications`, `password_reset_tokens`,
`persistent_logins`. Conversations are keyed by a participant-set
`conversationKey` (not by thread).

## Auth model
Session cookie (`JSESSIONID`) via form login (`email`/`password` at `POST /login`)
or Google OIDC; 30-day persistent remember-me; CSRF **enabled** (only
`/h2-console/**` and `/billing/webhook` exempt); everything not in the permit list
in `auth/SecurityConfig` requires authentication. There is **no token (bearer/JWT)
auth**, so a native client cannot authenticate today.

## Web vs. REST
The app is server-rendered only. There is no `/api/**` and no mobile REST layer.
The only non-HTML responses are `POST /chats/refresh` (JSON), `GET /chats/events`
(SSE), Stripe webhook, and PWA/SEO assets. See `API.md`.
