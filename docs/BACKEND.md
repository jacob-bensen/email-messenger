# BACKEND architecture (ConexusMail / email-messenger)

Reverse-engineered 2026-10-16. Unknowns are marked TBD.

## Stack (actual)
Java 21, Spring Boot 3, Spring Security, **Spring Data JPA/Hibernate** (NOT jOOQ — no codegen step),
Flyway (V1..V35 in `src/main/resources/db/migration`), PostgreSQL (prod/local), H2 (tests),
Thymeleaf, Jakarta Mail (IMAP/SMTP), Stripe (via gateway interfaces), Maven wrapper.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `domain` | JPA entities: User, Subscription, MailAccount, EmailThread, Message, Participant, Attachment, SavedSearch, Team*, auth/plan event rows |
| `repository` | Spring Data repositories |
| `service` | Conversation/IM transform: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ReplyService`, `ConversationKeyService`, view records (`ChatConversation`, `BubbleRun`, ...) |
| `email` | IMAP/Gmail integration: `MailboxPollingService`, `ImapIdleService`, `EmailImportService`, `MimeMessageParser`, `MailAccountService`, `CredentialEncryptor` |
| `web` | Thymeleaf controllers + `ChatService`, `ConversationListService`, `OutboundMessageService`, `SavedSearchService`, `ChatEventStream` (SSE), PWA/SEO |
| `auth` | Form login + Google OIDC, registration, verification, password reset, throttle, account settings |
| `billing` | Free/Pro/Business plans, Stripe checkout/portal/webhook, `PlanLimitService` |
| `team`, `digest`, `activation`, `reengagement`, `admin` | teams/invites, digests, lifecycle emails, operator metrics |

## Auth model
- Browser session (`JSESSIONID`) + persistent remember-me (JDBC token repo, 30 d); BCrypt passwords.
- CSRF **enabled**; exempt only `/h2-console/**`, `/billing/webhook` (Stripe signature).
- Everything not whitelisted in `SecurityConfig` requires authentication. Controllers resolve the user
  via `Principal` + `UserService.requireByEmail`.
- **There is no token/API-key auth. Mobile clients cannot authenticate today.**

## Web vs REST
There is **no REST API**. The only non-HTML responses: `POST /chats/refresh` (JSON `{imported:n}`),
`GET /chats/events` (SSE), `POST /billing/webhook`, PWA/SEO assets. See `docs/API.md`, `docs/WEB.md`.

## Business logic placement (drift risk)
`ConversationController` holds private helpers for mailbox scope selection (`selectMailbox`,
`scopeAddress`, `chatFor`, `populateList`). A future REST controller must reuse these, so they should be
moved into a service first (tracked in INTERNAL_TODO.md).

## Live delivery
`MailboxPollingService`/`ImapIdleService` publish `NewMailEvent` -> `ChatEventStream` -> SSE `new-mail`.
Web Push: not implemented (roadmap in CLAUDE.md).
