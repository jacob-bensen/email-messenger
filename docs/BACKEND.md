# BACKEND architecture (ConexusMail / email-messenger)

Reverse-engineered 2026-11-09 from the code. Unknowns are marked TBD.

## Stack
Java 21, Spring Boot 3, **Spring Data JPA** (not jOOQ — the generic routine
assumes jOOQ; this repo uses JPA entities + repositories), PostgreSQL 16 (H2 in
tests), Flyway (`src/main/resources/db/migration`, V1..V35, immutable), Thymeleaf,
Maven wrapper.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `domain` | JPA entities: User, MailAccount, EmailThread, Message, MessageRecipient, Participant, Attachment, SavedSearch, Subscription, Plan, Team*, token/audit entities |
| `repository` | Spring Data repositories |
| `email` | IMAP import/IDLE, Gmail OAuth, MIME parsing, credential encryption, polling scheduler |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), `ConversationService`, `ReplyService`, conversationKey logic |
| `web` | Thymeleaf controllers, `ChatService`, `OutboundMessageService`, SSE `ChatEventStream` |
| `auth` | Spring Security config, registration, Google OIDC, password reset, email verification, login throttle |
| `billing` | Stripe checkout/portal/webhook, plan limits |
| `team`, `digest`, `activation`, `reengagement`, `admin` | teams/invites, digests, activation & win-back mail, operator metrics |

## DB schema overview
users, mail_accounts, email_threads, messages, message_recipients, participants,
attachments (`blob_ref` always null today), saved_searches, subscriptions,
plan_change_events, teams, team_members, team_invites, thread_notes, auth_events,
email_verifications, password_reset_tokens, digest_email_preferences,
persistent_logins (remember-me).

## Auth model
Session cookie (form login, field `email`/`password`, or Google OIDC) + Spring
CSRF on all POSTs (exempt: `/billing/webhook`, `/h2-console/**`). Remember-me
(30 days, persistent token table). Everything except the public list in
`auth/SecurityConfig` requires authentication; unauthenticated requests redirect
to `/login`. **There is no token/JSON auth for a mobile client yet** (see
INTERNAL_TODO.md).

## Web vs REST
The app is entirely server-rendered. The only JSON/stream endpoints are
`POST /chats/refresh` (`{"imported": n}`) and the SSE stream `GET /chats/events`.
No `/api/**` namespace exists. Business logic lives in `service`/`web` service
classes (`ChatService`, `ConversationService`, `ReplyService`,
`OutboundMessageService`); controllers are thin wrappers. A future `/api/v1`
controller layer must call the same services.
