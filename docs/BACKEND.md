# BACKEND — architecture

Reverse-engineered from the code on 2026-10-10. "TBD" = not determined / not implemented.

## Stack (actual)
- Java 21, Spring Boot 3 (web, thymeleaf, data-jpa, validation, mail, security, oauth2-client, actuator), Maven wrapper.
- **Persistence is JPA/Hibernate (`ddl-auto: validate`) — not jOOQ.** Flyway migrations `V1`–`V35` in `src/main/resources/db/migration`. PostgreSQL in prod/local; H2 in tests.
- Stripe (`stripe-java`) for billing, Jakarta Mail for IMAP/SMTP, jsoup for body cleaning, GreenMail for tests.
- Docker and GitHub Actions CI were deliberately removed (see CLAUDE.md); do not re-add.

## Modules (`com.emailmessenger.*`)
| Package | Role |
|---|---|
| `auth` | Form login, Google OIDC, registration, password reset, email verification, remember-me, login throttle, account/appearance settings |
| `billing` | Free/Pro/Business plans, Stripe checkout/portal/webhook, plan limits (`PlanLimitService`) |
| `email` | IMAP import (`EmailImportService`, `MimeMessageParser`), polling (`MailboxPollingService`), IMAP IDLE, Gmail OAuth, encrypted mailbox credentials |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), conversation keys, `ReplyService` |
| `web` | Chats workspace, mailboxes, saved searches, landing/pricing/legal, PWA, SSE (`ChatEventStream`) |
| `team`, `digest`, `reengagement`, `activation`, `admin` | Teams/invites, digests, lifecycle emails, operator revenue/retention dashboards |
| `domain`, `repository` | JPA entities, Spring Data repositories |

## Data model overview
Tables: `users`, `subscriptions`, `plan_change_events`, `mail_accounts`, `email_threads` (owner + `conversation_key`), `messages`, `message_recipients`, `participants`, `attachments` (`blob_ref` always null today), `saved_searches`, `teams`, `team_members`, `team_invites`, `digest_email_preferences`, `password_reset_tokens`, `email_verifications`, `auth_events`, `persistent_logins`. A conversation (`/chats/{key}`) is the set of threads sharing a participant-set `conversationKey`.

## Auth model
- Session cookie (`JSESSIONID`) via form login (`POST /login`, fields `email`/`password`) or Google OIDC (only if `GOOGLE_CLIENT_ID` set). Remember-me cookie, 30 days.
- CSRF is **enabled** for everything except `/h2-console/**` and `/billing/webhook` (Stripe signature).
- Every route not in the permit-list (`SecurityConfig`) requires authentication; unauthenticated requests redirect to `/login` (HTML), there is no 401 JSON behaviour.
- `/admin/**` gated by `admin.emails` allowlist (non-admin gets 404).
- **There is no token/bearer auth for a mobile client (TBD).**

## Web vs. REST
The app is a server-rendered Thymeleaf site. **There is no JSON REST API for the mobile client.** The only non-HTML endpoints are listed in `API.md`. Controllers call services (`ChatService`, `ConversationListService`, `MailAccountService`, `OutboundMessageService`, ...), so a future REST layer must reuse those same services; some controller code (e.g. `ConversationController.send`, mailbox selection/scope helpers) still holds logic that would have to move into services first (see INTERNAL_TODO).
