# Backend architecture

Spring Boot 3 / Java 21 monolith ("ConexusMail"). Server-rendered Thymeleaf website
plus background jobs. **There is no JSON REST API yet** — see `API.md`.

## Stack (as implemented)
- Persistence: **Spring Data JPA/Hibernate** (NOT jOOQ), PostgreSQL 16 prod, H2 in tests.
- Migrations: Flyway, `src/main/resources/db/migration/V1..V35` (immutable once merged).
- Security: Spring Security — form login + Google OIDC, remember-me (`persistent_logins`),
  CSRF on (ignored for `/billing/webhook`, `/h2-console/**`), login throttle filter.
- Billing: Stripe (Free/Pro/Business), plan limits via `PlanLimitService`.
- Mail: Jakarta Mail IMAP import + IDLE, SMTP send; Gmail via OAuth.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers, view services (`ChatService`, `ConversationListService`, `OutboundMessageService`, `OnboardingService`), SSE (`ChatEventStream`) |
| `service` | Conversation/IM transform (`ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ReplyService`, `ConversationKeyService`) |
| `domain` | JPA entities (User, MailAccount, EmailThread, Message, Participant, Attachment, Subscription, Team*, SavedSearch, ...) |
| `repository` | Spring Data repositories |
| `email` | IMAP/IDLE/polling/import, Gmail OAuth, credential encryption |
| `auth` | Security config, registration, password reset, email verification, account pages |
| `billing` | Stripe checkout/portal/webhook, plans, limits, trial nudges |
| `admin`, `digest`, `activation`, `reengagement`, `team` | Metrics, digests, lifecycle emails, teams |

## Schema overview
`users`, `subscriptions`, `mail_accounts` (per-user IMAP/Gmail accounts, polling cursors
`last_seen_uid`/`last_seen_sent_uid`), `email_threads` (owner, `conversation_key` = participant set),
`messages` (+ outbound flag), `message_recipients`, `participants`, `attachments`
(`blob_ref` always null today), `saved_searches`, `digest_email_preferences`, `teams`,
`team_members`, `team_invites`, `auth_events`, `password_reset_tokens`, `email_verifications`,
`plan_change_events`, `persistent_logins`.

## Auth model
Session cookie (form login or Google OIDC). All routes authenticated except the public list in
`auth/SecurityConfig`. No token/bearer auth exists — **mobile client auth is TBD** (see INTERNAL_TODO).

## Web vs API
Controllers call services directly; no JSON API layer exists, so no web/API logic divergence yet.
When `/api/v1` is added it MUST reuse `ChatService`/`ConversationService`/`OutboundMessageService`.
