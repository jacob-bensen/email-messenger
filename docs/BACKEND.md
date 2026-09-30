# BACKEND.md — architecture

Repo classification: **BACKEND** (evidence: `pom.xml`, Spring Boot annotations, Flyway migrations
`V1`–`V35`, Thymeleaf templates). Sibling: `conexus-mail-mobile` (FRONTEND, Flutter).

## Stack (as implemented)
Java 21, Spring Boot 3, Spring Data **JPA/Hibernate** (`ddl-auto: validate`; **not jOOQ**, despite the
generic routine template), PostgreSQL 16 (prod/local), H2 (tests), Flyway, Thymeleaf, Spring Security,
Stripe, Jakarta Mail (IMAP/SMTP), Maven wrapper.

## Package layout (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers: chats workspace, mailboxes, saved searches, pricing/landing, legal, SEO, PWA, SSE stream (`ChatEventStream`) |
| `auth` | Form login, Google OIDC, registration, email verification, password reset, account settings, login throttling |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), trial nudges |
| `email` | IMAP polling/IDLE, Gmail OAuth, MIME parsing, import, credential encryption |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), conversation view models, `ReplyService`, `ConversationKeyService` |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams + invites, digests, lifecycle emails, admin revenue/retention metrics |

## Domain model
`User`, `Subscription`/`Plan`, `MailAccount` (IMAP password or Gmail OAuth, encrypted creds, polling policy,
signature), `EmailThread`, `Message`, `MessageRecipient`, `Participant`, `Attachment` (metadata only; `blob_ref`
always null today), `SavedSearch`, `Team`/`TeamMember`/`TeamInvite`, `DigestEmailPreference`, auth/plan audit
events, token tables. A *chat* is keyed by `conversationKey` (participant-set), merging several threads.

## Auth model
Session cookie (form login at `/login`, remember-me, optional Google OIDC). CSRF is **enabled** everywhere
except `/billing/webhook` (Stripe signature-verified) and `/h2-console/**`. Login throttled by
`LoginThrottleFilter`. **No token/API-key auth exists** — there is no mobile-facing auth yet (see INTERNAL_TODO).

## Web vs REST
There is currently **no REST API**. The only `@RestController` is the Stripe webhook. All user-facing
behaviour is server-rendered; controllers call services (`ChatService`, `ConversationListService`,
`OutboundMessageService`, `MailAccountService`, ...). A future `/api/v1` must reuse these services
(see `docs/API.md`).
