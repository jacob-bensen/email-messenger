# BACKEND.md — architecture

Reverse-engineered 2026-11-01 from the code. Anything not verifiable is marked TBD.

## Stack
Java 21, Spring Boot 3.5.x, Spring MVC + Thymeleaf, Spring Data **JPA** (not jOOQ —
the generic routine assumed jOOQ; this repo uses JPA entities/repositories), Flyway
(35 migrations, `src/main/resources/db/migration`, V1..V35), PostgreSQL 16 (prod/local),
H2 (tests), Maven wrapper. Stripe for billing, Jakarta Mail for IMAP/SMTP.

## Package layout (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats workspace, mailboxes, pricing, PWA, SEO, legal, saved searches), `ChatService`, `OutboundMessageService`, SSE `ChatEventStream` |
| `service` | Conversation/IM transform (`IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`, `ConversationService`) |
| `domain` | JPA entities (User, MailAccount, EmailThread, Message, Participant, Attachment, Subscription, Team*, SavedSearch, ...) |
| `repository` | Spring Data repositories |
| `email` | IMAP import/polling/IDLE, Gmail OAuth, `MimeMessageParser`, `CredentialEncryptor` |
| `auth` | Security config, form + Google OIDC login, registration, verification, password reset, throttling |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | teams/invites, digest mail, lifecycle schedulers, admin revenue metrics |

## Data model overview
Participants (deduped by normalized email) → Messages (sender, recipients TO/CC/BCC,
plain+html body, attachments) → EmailThread (grouped by Message-ID/In-Reply-To/References,
owned by a user, carries `conversation_key` = participant-set key, unread flag).
`/chats` conversations are keyed by `conversationKey`, merging multiple threads.
MailAccount holds encrypted IMAP creds or Gmail OAuth tokens, UID cursors (inbox + sent).
Plans: `FREE`, `PRO`, `BUSINESS` with per-plan limits.

## Auth model
Session cookie (JSESSIONID) + persistent remember-me (30d, JDBC token repo). Form login at
`POST /login` (params `email`, `password`), optional Google OIDC (enabled only when
`GOOGLE_CLIENT_ID` is set). CSRF **enabled** except `/billing/webhook` and `/h2-console/**`.
Login throttling filter. Everything not whitelisted in `SecurityConfig` requires auth.
**There is no token/API-key auth today** — the mobile client has nothing to authenticate with.

## Web vs REST
The app is server-rendered. The only JSON/stream endpoints are `POST /chats/refresh`
(`{"imported":n}`) and the SSE stream `GET /chats/events`. No `/api/**` exists yet.
See `API.md` for the current surface and the TBD mobile contract.
