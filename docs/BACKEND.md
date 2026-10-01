# BACKEND.md — architecture

Repo classification: **BACKEND** (pom.xml + Spring Boot + Flyway). It also serves the public
website via Thymeleaf. Sibling repo: `conexus-mail-mobile` (Flutter client, currently empty scaffold).

## Stack (as implemented)
Java 21, Spring Boot 3 (web, thymeleaf, data-jpa, validation, mail, security, oauth2-client, actuator),
PostgreSQL (prod/local) / H2 (tests), Flyway (`src/main/resources/db/migration`, V1..V35),
Jakarta Mail (IMAP/SMTP), Stripe Java, jsoup, GreenMail + Testcontainers in tests.
**Persistence is Spring Data JPA, not jOOQ** (the generic routine template assumes jOOQ; no codegen step exists).

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, landing/legal/SEO/PWA), `ChatService`, `ConversationListService`, `OutboundMessageService`, SSE `ChatEventStream` |
| `service` | Conversation/IM transform: `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`, view models (`ChatConversation`, `BubbleRun`, …) |
| `email` | Mailbox integration: IMAP client, polling (`MailboxPollingService`), IMAP IDLE, Gmail OAuth, MIME parsing, credential encryption |
| `domain` / `repository` | JPA entities / Spring Data repositories |
| `auth` | Security config, registration, login throttling, Google OIDC, email verification, password reset, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), trial-end nudges |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, weekly digests, drip emails, operator revenue/retention dashboards |

## Schema overview (tables)
users, subscriptions, plan_change_events, auth_events, email_verifications, password_reset_tokens,
persistent_logins (remember-me), mail_accounts, email_threads, messages, message_recipients,
participants, attachments (`blob_ref` always null today), saved_searches, teams, team_members,
team_invites, digest_email_preferences. (`thread_notes` created then dropped in V33.)
Conversations in the UI are keyed by participant-set `conversationKey`, not by thread.

## Auth model
Browser session (JSESSIONID) via form login (`POST /login`, params `email`/`password`) or Google OIDC
(only when `GOOGLE_CLIENT_ID` set); remember-me cookie (30 d, JDBC token repo). CSRF is **enabled**
except `/h2-console/**` and `/billing/webhook`. Everything not on the permit-list in `SecurityConfig`
requires authentication. **No bearer/API-token auth exists** — a mobile client cannot authenticate
today without a new mechanism (see INTERNAL_TODO.md).

## Web vs REST
There is no REST API. All user-facing behavior is server-rendered Thymeleaf; the only JSON/stream
endpoints are `POST /chats/refresh`, `GET /chats/events` (SSE) and `POST /billing/webhook`
(see API.md). Controllers delegate to services (`ChatService`, `OutboundMessageService`,
`MailAccountService`, …), so a future `/api/v1` layer must reuse those same services.

## Ops docs
`master/` holds the product-owner planning files (PLAN.md, BACKLOG.md, MASTER_ACTIONS.md, and a large
historical CHANGELOG.md). `DEPLOY.md` covers deployment.
