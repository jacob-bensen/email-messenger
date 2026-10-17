# BACKEND — architecture

Spring Boot 3 / Java 21 server-rendered app ("ConexusMail"). Single deployable: Thymeleaf website +
(future) REST API for the Flutter client in `conexus-mail-mobile`.

> Deviation from the generic target stack: persistence is **Spring Data JPA** (not jOOQ). There is no jOOQ
> codegen step. Flyway migrations (`src/main/resources/db/migration`, V1–V35) are the schema source of truth.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats workspace, mailboxes, pricing/landing, PWA, SEO, SSE `/chats/events`), chat/list services |
| `service` | Conversation view models, IM transform, `EmailBodyCleaner`, `ReplyService`, `ConversationKeyService` |
| `email` | IMAP/Gmail integration: import, polling, IDLE, MIME parsing, credential encryption |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `auth` | Form login + Google OIDC, registration, password reset, email verification, throttling, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), trial conversion |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digests, lifecycle emails, `/admin/revenue` |

## Schema overview
Users/subscriptions (V2, V4, V17, V27–28), mail accounts (V5–7, V29, V31, V35), threads/messages/participants/
attachments (V1, V3, V9, V30, V32), saved searches (V10–11), digests (V12), auth tokens/events (V14–16, V18–19),
lifecycle tracking (V13, V20–23), teams (V24), plan change events (V26), user mail prefs (V34).
Conversations are keyed by participant-set `conversationKey` (V32) rather than by thread.

## Auth model
Session cookie (`JSESSIONID`) + persistent remember-me (30 d), CSRF **enabled** (exempt: `/h2-console/**`,
`/billing/webhook`). Form login at `POST /login` (`email`,`password`); optional Google OIDC when `GOOGLE_CLIENT_ID`
is set. Everything not in the permit-list in `auth/SecurityConfig` requires authentication.
**There is no token/bearer auth yet** — a mobile client cannot authenticate except by emulating the browser form
flow. See `INTERNAL_TODO.md`.

## Web vs REST
Controllers are thin: they resolve the `User` from `Principal` and call services (`ChatService`,
`ConversationListService`, `OutboundMessageService`, `ReplyService`, `MailboxPollingService`, `BillingService`).
Any future `/api/v1` controller MUST call the same services (see `API.md`).
