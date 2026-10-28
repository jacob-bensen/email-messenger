# BACKEND.md — architecture (reverse-engineered 2026-10-28)

Repo: `email-messenger` ("ConexusMail"). Spring Boot 3 / Java 21 / Maven, server-rendered Thymeleaf.
Persistence is **Spring Data JPA + Hibernate** (`ddl-auto: validate`) on PostgreSQL 16 (H2 in tests), schema by Flyway
(`src/main/resources/db/migration`, `V1`…, immutable once merged). **There is no jOOQ in this project** (the generic
routine template assumes it; see INTERNAL_TODO).

## Modules (`com.emailmessenger.*`)
| Package | Role |
|---|---|
| `auth` | Form login + Google OIDC, registration, password reset, email verification, remember-me (JDBC token store), login throttle, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`), trial nudges |
| `email` | IMAP import/polling/IDLE (`MailboxPollingService`, `ImapIdleService`), MIME parsing, Gmail OAuth, credential encryption |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), conversation keys, `ReplyService`, chat view models |
| `web` | Thymeleaf MVC controllers, chat workspace, mailbox mgmt, saved searches, PWA, SEO, SSE (`ChatEventStream`) |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digests, lifecycle emails, operator metrics (`/admin/**`, allowlist `admin.emails`) |

## Domain
`User`, `MailAccount` (IMAP/Gmail-OAuth, encrypted creds), `EmailThread`, `Message`, `MessageRecipient`, `Participant`,
`Attachment` (metadata only; `blob_ref` null), `SavedSearch`, `Subscription`, `PlanChangeEvent`, `Team*`, `AuthEvent`,
`EmailVerificationToken`, `PasswordResetToken`, `DigestEmailPreference`. A *chat* is keyed by participant-set
`conversationKey` (`ConversationKeyService`), merging multiple email threads.

## Auth model
Session cookie (`JSESSIONID`) + optional remember-me cookie, CSRF **enabled** (exempt: `/billing/webhook`, `/h2-console/**`).
Everything except the public list in `SecurityConfig` requires authentication; unauthenticated requests redirect to
`/login` (HTML). **No token/bearer auth exists**, so a mobile client cannot authenticate today.

## Web vs REST
There is no REST API. Controllers are `@Controller` returning views; only JSON endpoint is `POST /chats/refresh`
(`{"imported":n}`), plus SSE `/chats/events` and the Stripe webhook. See `API.md` for the real surface and the
proposed mobile contract status, `WEB.md` for pages. Business logic lives in services (`ChatService`,
`ConversationListService`, `OutboundMessageService`, `MailAccountService`, …); controllers only adapt.

## Other docs
`CLAUDE.md` (conventions + roadmap), `DEPLOY.md`, `master/` (PLAN/BACKLOG/CHANGELOG/MASTER_ACTIONS from an earlier
operator-driven routine; `master/CHANGELOG.md` is the historical log).
