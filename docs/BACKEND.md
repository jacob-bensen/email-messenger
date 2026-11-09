# BACKEND.md — architecture (reverse-engineered 2026-11-09)

Repo classification: BACKEND (pom.xml, Spring Boot 3, Flyway `db/migration/V1..V35`).
Note: the target stack in the sync routine says jOOQ; this repo uses **Spring Data JPA** (Hibernate), no jOOQ, no codegen.

## Stack
Java 21, Spring Boot 3.x, web + thymeleaf + data-jpa + validation + mail + security + oauth2-client + actuator,
PostgreSQL (prod/local) / H2 (tests), Flyway, jsoup, Jakarta Mail, Stripe SDK. Maven wrapper (`./mvnw`).

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`ConversationController` = /chats, `MailboxController`, `GmailConnectController`, `SavedSearchController`, `PricingController`, `PwaController`, `SeoController`, `LegalController`, `DemoController`), `ChatEventsController` (SSE), view services (`ChatService`, `ConversationListService`, `OutboundMessageService`) |
| `service` | Conversation domain logic: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`; view-model records (`ChatConversation`, `ConversationListItem`, `BubbleRun`, ...) |
| `email` | Mailbox integration: IMAP import (`EmailImportService`, `MimeMessageParser`), polling (`MailboxPollingService/Scheduler`), IMAP IDLE (`ImapIdleService`), Gmail OAuth (`GmailOAuthClient`), credential encryption |
| `auth` | Form login + Google OIDC, registration, password reset, email verification, login throttle, account pages |
| `billing` | Stripe checkout/portal/webhook, plan limits (Free/Pro/Business), trial nudges |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, weekly digests, lifecycle emails, admin revenue/retention metrics |
| `domain`, `repository` | JPA entities, Spring Data repositories |

## DB schema overview (Flyway, immutable once merged)
Tables: `users`, `subscriptions`, `plan_change_events`, `persistent_logins`, `auth_events`, `password_reset_tokens`,
`email_verifications`, `mail_accounts`, `participants` (dedup by normalized email), `email_threads`
(owner, `conversation_key`, unread), `messages` (incl. outbound flag V30), `message_recipients`, `attachments`
(`blob_ref` always null — bytes not stored), `saved_searches`, `digest_email_preferences`, `teams`, `team_members`,
`team_invites`. `thread_notes` was created in V25 and dropped in V33. Next migration number: **V36**.

## Auth model
Session cookie (`JSESSIONID`) + persistent remember-me (`persistent_logins`), CSRF enabled
(ignored only for `/billing/webhook`, `/h2-console/**`). Form login at `/login`; Google OIDC when client
registration env vars are present. Everything except an explicit public allowlist
(`SecurityConfig`) requires authentication. **There is no token/Bearer auth and no stateless API.**
Admin pages gated by `AdminAuthorizer` (allowlist; non-admins get 404).

## Web vs REST
The app is entirely server-rendered. The only non-HTML responses are: SSE `/chats/events`, JSON
`POST /chats/refresh`, `POST /billing/webhook` (Stripe), PWA/SEO assets. **No REST API exists for a mobile
client** — see `API.md` for the exact inventory and the (TBD) plan.
Controllers resolve the user via `UserService.requireByEmail(principal)` and delegate to services, so a
future `/api/v1` controller layer should reuse `ChatService`, `ConversationListService`, `ReplyService`,
`OutboundMessageService`, `MailAccountService`.
