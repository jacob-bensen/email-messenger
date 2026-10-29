# Backend architecture

Reverse-engineered from the code on 2026-10-29. Unknowns are marked TBD.

## Stack
Java 21, Spring Boot 3, Spring Data **JPA** (not jOOQ), PostgreSQL 16 (H2 in
tests), Flyway (`src/main/resources/db/migration`, V1..V35), Thymeleaf,
Spring Security (form login + Google OIDC), Stripe, Jakarta Mail (IMAP/SMTP).

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, pricing, legal, PWA, SEO), `ChatService`, `ConversationListService`, `OutboundMessageService`, SSE `ChatEventStream` |
| `service` | Conversation building: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`, view records |
| `domain` / `repository` | JPA entities and Spring Data repositories |
| `email` | IMAP import/polling/IDLE, Gmail OAuth, `MimeMessageParser`, credential encryption |
| `auth` | Registration, login, password reset, email verification, Google OIDC, account settings, throttling |
| `billing` | Stripe checkout/portal/webhook, plan limits |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digest emails, lifecycle emails, admin metrics |

## Data model (overview)
`User`, `Subscription` (Plan Free/Pro/Business), `MailAccount` (IMAP password or
Gmail OAuth; polling cursors `last_seen_uid`, `last_seen_sent_uid`),
`EmailThread` (owner + `conversation_key` = participant set), `Message`
(`outbound` flag), `MessageRecipient`, `Participant`, `Attachment`
(`blob_ref` always null today), `SavedSearch`, `Team`/`TeamMember`/`TeamInvite`,
`AuthEvent`, `PlanChangeEvent`, verification/reset tokens.

## Auth model
Session cookie (`JSESSIONID`) + persistent remember-me (30 days, JDBC token
repo). CSRF is **enabled** except `/h2-console/**` and `/billing/webhook`.
Everything not in the permit-all list (see `SecurityConfig`) requires login.
There is **no token/bearer auth** — a mobile client cannot authenticate
without a new mechanism (see INTERNAL_TODO).

## Web vs REST
The only JSON/`@RestController` endpoint is the Stripe webhook. All user
features are server-rendered Thymeleaf (see `WEB.md`). There is no mobile
REST API yet; `docs/API.md` records that and the proposed shape.
