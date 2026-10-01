# Backend architecture

Reverse-engineered 2026-10-01. Anything not implemented is marked TBD.

## Stack
Java 21, Spring Boot 3, Spring Data **JPA** (not jOOQ), PostgreSQL (H2 in tests), Flyway
(`src/main/resources/db/migration`, V1–V35), Thymeleaf, Maven wrapper. Stripe (billing),
Jakarta Mail (IMAP/SMTP), Spring Security (form login + Google OIDC + remember-me).

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web/` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, landing/legal/PWA/SEO), SSE (`ChatEventStream`), view services |
| `service/` | Conversation keying, IM transform, body cleaning, reply sending (`ReplyService`) |
| `email/` | IMAP import/polling/IDLE, Gmail OAuth, MIME parsing, credential encryption |
| `domain/`, `repository/` | JPA entities / Spring Data repositories |
| `auth/` | Security config, registration, password reset, email verification, Google OIDC |
| `billing/` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`) |
| `team/`, `digest/`, `admin/`, `activation/`, `reengagement/` | Teams/invites, digests, admin metrics, lifecycle emails |

## Data model (overview)
`User`, `MailAccount` (IMAP password or Gmail OAuth), `EmailThread`, `Message`,
`MessageRecipient`, `Participant`, `Attachment` (`blob_ref` always null today),
`SavedSearch`, `Subscription`/`PlanChangeEvent`, `Team`/`TeamMember`/`TeamInvite`,
`AuthEvent`, token tables. Conversations are keyed by participant-set `conversationKey`.

## Auth model
Session cookie (`JSESSIONID`) via form login (`/login`, params `email`/`password`) or Google
OIDC; remember-me cookie (30 d, persistent token table). **CSRF is enabled** for everything
except `/billing/webhook` and `/h2-console/**`. Everything not on the public allow-list in
`SecurityConfig` requires authentication. There is **no token (bearer/JWT) auth** — TBD for mobile.

## Web vs REST
The backend is currently a server-rendered web app. The only JSON/stream endpoints are
`POST /chats/refresh`, `GET /chats/events` (SSE) and `POST /billing/webhook`. **There is no REST
API for a mobile client yet** (see `INTERNAL_TODO.md`). When added, controllers must call the same
services as the Thymeleaf controllers (`ConversationListService`, `ChatService`, `ReplyService`,
`OutboundMessageService`, `MailAccountService`).
