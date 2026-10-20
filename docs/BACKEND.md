# BACKEND

Spring Boot 3 / Java 21 monolith ("ConexusMail"). Server-rendered Thymeleaf web app.
Reverse-engineered from code on 2026-10-20; unknowns are marked TBD.

> Deviation from the target stack: persistence is **Spring Data JPA/Hibernate**, not jOOQ
> (no jOOQ dependency in `pom.xml`, no codegen). Flyway + PostgreSQL 16 (H2 in tests) as expected.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`ConversationController`, `MailboxController`, `GmailConnectController`, `SavedSearchController`, `PricingController`, `PwaController`, `SeoController`, `LegalController`, `ChatEventsController`), view services (`ChatService`, `ConversationListService`, `OutboundMessageService`, `OnboardingService`) |
| `service` | Conversation domain logic: `ConversationKeyService` (participant-set key), `IMTransformService`, `EmailBodyCleaner`, `ReplyService`, view models (`ChatConversation`, `BubbleRun`, ...) |
| `email` | Mailbox integration: `MimeMessageParser`, `EmailImportService`, `MailboxPollingService`/`Scheduler`, `ImapIdleService`, `GmailOAuthClient`, `CredentialEncryptor` |
| `auth` | Security (`SecurityConfig`), registration/login/reset/verify, Google OIDC, account settings, login throttle/audit |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`), banners, trial nudges |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Team invites; weekly digests; activation/re-engagement schedulers; `/admin/revenue` metrics |
| `domain`, `repository` | JPA entities / Spring Data repositories |

## Schema (Flyway `src/main/resources/db/migration`, V1..V35, immutable once merged)
Tables: users, subscriptions, plan_change_events, mail_accounts, email_threads, messages,
message_recipients, participants, attachments (`blob_ref` always null today), thread_notes,
saved_searches, teams, team_members, team_invites, digest_email_preferences,
email_verifications, password_reset_tokens, persistent_logins, auth_events.
Conversations ("chats") are keyed by `conversationKey` = normalized participant set, not by thread.

## Auth model
- Form login (`/login`, params `email`/`password`), remember-me cookie (30d, persistent token table),
  optional Google OIDC (`/auth/google/start`). **Session cookie (JSESSIONID) + CSRF token** for all
  mutating requests; CSRF is ignored only for `/billing/webhook` (Stripe signature) and `/h2-console/**`.
- Everything not in the `permitAll` list in `SecurityConfig` requires an authenticated session.
- **No token/API-key auth exists.** A mobile client cannot authenticate today (see INTERNAL_TODO).

## Web vs REST
There is **no REST API for a mobile client**. The only JSON/stream endpoints are `POST /chats/refresh`,
`GET /chats/events` (SSE) and `POST /billing/webhook`; see `API.md`. Web controllers delegate to the
service layer (`ChatService`, `ReplyService`, `MailAccountService`, ...). A future `/api/v1` layer must
reuse those services rather than the controllers.

## Build / test
`./mvnw -q -B verify`; tests use H2 (`src/test/resources/application.yml`). Historical per-feature notes
live in `master/CHANGELOG.md`, `master/PLAN.md`, `master/BACKLOG.md`, `master/MASTER_ACTIONS.md`.
