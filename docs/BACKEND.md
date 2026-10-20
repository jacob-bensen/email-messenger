# BACKEND.md — ConexusMail server architecture

Reverse-engineered from the code on 2026-10-20. Unknowns are marked TBD.

## Stack
Java 21, Spring Boot 3 (web, thymeleaf, data-jpa, validation, mail, security, oauth2-client, actuator),
PostgreSQL (prod/local), H2 (tests), Flyway (35 migrations, `V1`..`V35`), Maven wrapper.
**Persistence is Spring Data JPA, not jOOQ** — there is no jOOQ codegen step.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, landing/pricing/legal/PWA/SEO), `ChatService`, `ConversationListService`, `OutboundMessageService`, SSE `ChatEventStream` |
| `auth` | Security config, form login + Google OIDC, registration, password reset, email verification, account settings, login throttling/audit |
| `billing` | Stripe checkout/portal/webhook, plans & `PlanLimitService` (Free/Pro/Business), trial-end conversion |
| `email` | IMAP import (`EmailImportService`, `MimeMessageParser`), polling (`MailboxPollingService`), IMAP IDLE, Gmail OAuth, credential encryption |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), `ConversationService`, `ConversationKeyService`, `ReplyService` |
| `domain` / `repository` | JPA entities / Spring Data repositories |
| `team`, `digest`, `admin`, `activation`, `reengagement` | Teams + invites, digests, operator metrics, onboarding and win-back |

## Schema overview (tables)
`users`, `subscriptions`, `plan_change_events`, `mail_accounts` (IMAP/Gmail creds, encrypted), `participants`,
`email_threads`, `messages`, `message_recipients`, `attachments` (metadata only; `blob_ref` always null),
`thread_notes`, `saved_searches`, `teams`, `team_members`, `team_invites`, `auth_events`,
`password_reset_tokens`, `email_verifications`, `persistent_logins`, `digest_email_preferences`.
A conversation is not a table: it is the participant-set `conversationKey` computed by `ConversationKeyService`
and stored on threads.

## Auth model
Session-cookie auth (`JSESSIONID`) + 30-day remember-me; CSRF **enabled** (only `/billing/webhook` and
`/h2-console/**` exempt). Form login (`POST /login`, params `email`, `password`) and optional Google OIDC.
Everything not on the public allow-list in `SecurityConfig` requires authentication; unauthenticated requests are
redirected to `/login` (HTML), not answered with 401. **There is no token/bearer auth for a mobile client.**

## Web vs REST
There is no REST API today. All user-facing behavior is server-rendered Thymeleaf; controllers resolve the user
with `userService.requireByEmail(principal.getName())` and delegate to services. The only JSON/stream endpoints are
`POST /chats/refresh` (returns `{"imported": n}`) and `GET /chats/events` (SSE). See `API.md` for the
proposed mobile API, which must call the same services (`ConversationListService`, `ChatService`,
`OutboundMessageService`, `MailAccountService`, `PlanLimitService`).
