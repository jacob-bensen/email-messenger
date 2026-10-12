# BACKEND.md — architecture

Reverse-engineered from the code on 2026-10-12. Items not verified are marked TBD.

## Stack (actual)
Java 21, Spring Boot 3, **Spring Data JPA (Hibernate) — not jOOQ**, Flyway
(`src/main/resources/db/migration`, V1–V35), PostgreSQL (H2 in tests), Thymeleaf,
Spring Security (form login + Google OIDC + remember-me), Stripe, Jakarta Mail (IMAP/SMTP),
jsoup. Maven wrapper (`./mvnw`).

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers, chat/mailbox/saved-search/PWA/SEO, `ChatEventStream` (SSE) |
| `service` | Conversation building: `ConversationService`, `ChatService`-feeding records (`ChatConversation`, `BubbleMessage`, `ChatMember`), `IMTransformService`, `EmailBodyCleaner`, `ReplyService`, `ConversationKeyService` |
| `email` | IMAP polling/IDLE, Gmail OAuth, MIME parsing, import (`EmailImportService`), mailbox accounts, credential encryption |
| `domain` | JPA entities (User, MailAccount, EmailThread, Message, Participant, Attachment, Subscription, Team, SavedSearch, …) |
| `repository` | Spring Data repositories |
| `auth` | SecurityConfig, registration, login throttling, password reset, email verification, Google OIDC |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | teams/invites, weekly digests, lifecycle emails, operator metrics |

## Data model overview
`User` 1—* `MailAccount` (IMAP password or Gmail OAuth, `AuthType`). Imported mail →
`EmailThread` (owner, `conversationKey` = participant-set key, V32) 1—* `Message`
(sender, recipients, plain+html, `outbound` flag V30) 1—* `Attachment` (`blob_ref` always
null today — attachments are not downloadable). `Participant` deduped by normalized email.
`Subscription` + `PlanChangeEvent` for billing; `Team`/`TeamMember`/`TeamInvite`;
`SavedSearch`; `AuthEvent`; token tables for reset/verify.

## Auth model
Session cookie (`JSESSIONID`) + persistent remember-me (JDBC token repo, 30 days).
Everything except an allow-list in `SecurityConfig` requires authentication. **CSRF is on**
(exempt: `/h2-console/**`, `/billing/webhook`). There is **no token/bearer authentication**
and no `/api/**` surface — a mobile client cannot authenticate today.

## Web vs REST
There is currently **no REST API**. All endpoints are server-rendered MVC returning
Thymeleaf views/redirects, plus one SSE stream (`/chats/events`) and PWA assets.
Controllers delegate to services (`ConversationListService`, `ChatService`, `ReplyService`,
`OutboundMessageService`, `MailAccountService`, `MailboxPollingService`), which is the
layer a future `/api/v1` must reuse. See `API.md` for the proposed gap and `WEB.md` for pages.

## Process docs
`master/` (PLAN.md, BACKLOG.md, CHANGELOG.md, MASTER_ACTIONS.md) is a pre-existing
planning log from an earlier routine; `master/CHANGELOG.md` is large history. New entries go in root `CHANGELOG.md`.
