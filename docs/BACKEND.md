# Backend architecture

Repo classification: **BACKEND** (evidence: `pom.xml`, Spring Boot 3 / Java 21, 35 Flyway migrations in `src/main/resources/db/migration`, `@Controller`s, Thymeleaf templates).
Sibling: `conexus-mail-mobile` (FRONTEND, Flutter — currently only a README).

## Deviations from the generic target stack
- Persistence is **Spring Data JPA/Hibernate** (`ddl-auto: validate`), **not jOOQ**. There is no codegen step. Flyway owns the schema.
- Tests run on H2 (`src/test/resources/application.yml`); Testcontainers/GreenMail are used for integration tests.

## Package layout (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, legal, SEO, PWA), `ChatService`, `ConversationListService`, SSE `ChatEventStream` |
| `auth` | Form login + Google OIDC, registration, password reset, email verification, account settings, `SecurityConfig` |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), trial nudges |
| `email` | IMAP import/polling/IDLE, Gmail OAuth, `MimeMessageParser`, credential encryption |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), `ReplyService`, conversation view records |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `team`, `digest`, `admin`, `activation`, `reengagement` | teams+invites, digests, admin metrics, lifecycle emails |

## Schema overview (Flyway V1–V35)
Core: `users`, `subscriptions`, `mail_accounts` (IMAP password or Gmail OAuth, encrypted creds, polling policy, last-seen UIDs), `participants`, `email_threads` (owner, `conversation_key` = participant-set key, unread), `messages` (+`outbound`), `message_recipients`, `attachments` (`blob_ref` always null today).
Other: `saved_searches`, `digest_email_preferences`, `password_reset_tokens`, `email_verifications`, `auth_events`, `persistent_logins`, `teams`/`team_members`/`team_invites`, `plan_change_events`. `thread_notes` was created in V25 and dropped in V33.
Migrations are immutable once merged; add a new `V{n}`.

## Auth model
Session-cookie auth (Spring Security form login `/login`, optional Google OIDC when `GOOGLE_CLIENT_ID` is set, remember-me 30 days, CSRF **on**; only `/h2-console/**` and `/billing/webhook` are exempt). Everything not in the `permitAll` list in `SecurityConfig` requires login. Login is throttled (`LoginThrottleFilter`). **There is no token/bearer auth, so a mobile client cannot authenticate yet** (see INTERNAL_TODO).

## Web vs. API
There is currently **no REST API**. The only non-HTML endpoints are the Stripe webhook (`@RestController`), `POST /chats/refresh` (JSON) and the SSE stream `/chats/events`. Chat data reaches the browser as Thymeleaf model attributes built from records in `service`/`web` (`ConversationListItem`, `ChatConversation`, `BubbleRun`, `BubbleMessage`). A future `/api/v1` must call the same services (`ConversationListService`, `ChatService`, `ReplyService`/`OutboundMessageService`, `MailAccountService`) rather than the controllers. Note `ConversationController` still holds orchestration logic (account scoping, signature append, reply-vs-new send, attachment conversion) that must first be extracted into a service — see INTERNAL_TODO.
