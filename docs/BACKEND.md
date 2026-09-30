# Backend architecture

Repo role: **BACKEND** (evidence: `pom.xml`, Spring Boot 3 / Java 21, Flyway migrations in
`src/main/resources/db/migration` V1–V35, Thymeleaf templates). Product name: ConexusMail.

## Stack (as implemented)
- Spring Boot web + Thymeleaf + **Spring Data JPA** (entities in `domain/`, repositories in
  `repository/`). **jOOQ is NOT used** — there is no jOOQ codegen step. PostgreSQL in prod, H2 in tests.
- Flyway migrations are immutable once merged; add a new `V{n}__*.sql`.
- Build/test: `./mvnw test` (see `CLAUDE.md`).

## Package layout (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`ConversationController` = /chats workspace, `MailboxController`, `PricingController`, …), `ChatEventStream` (SSE), view services (`ChatService`, `ConversationListService`, `OutboundMessageService`) |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), conversation keys (`ConversationKeyService`), `ReplyService`, view records (`ChatConversation`, `BubbleRun`, …) |
| `email` | IMAP/Gmail integration: import (`EmailImportService`, `MimeMessageParser`), polling, IMAP IDLE, credential encryption |
| `auth` | Form login + Google OIDC, registration, password reset, email verification, throttling |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, weekly digest, lifecycle emails, operator metrics |

## Schema overview (tables)
users, subscriptions, plan_change_events, mail_accounts, email_threads (owner-scoped,
`conversation_key` = participant-set key), messages, message_recipients, participants,
attachments (`blob_ref` always null today — bytes are not stored), saved_searches, teams,
team_members, team_invites, auth_events, email_verifications, password_reset_tokens,
persistent_logins, digest_email_preferences. (`thread_notes` dropped in V33.)

## Auth model
Session cookie (JSESSIONID) via form login (`/login`, params `email`/`password`), optional
remember-me (30d), Google OIDC when `GOOGLE_CLIENT_ID` is set. **CSRF is enabled** except
`/billing/webhook` and `/h2-console/**`. Everything not on the permit-list in `SecurityConfig`
requires authentication. **There is no token-based (Bearer/JWT) auth** — a mobile client
cannot authenticate today. TBD: design (see `INTERNAL_TODO.md`).

## Web vs REST
The only `@RestController` is `BillingWebhookController` (Stripe). All user-facing endpoints
return server-rendered HTML or redirects. **No REST API for the mobile client exists yet**;
`docs/API.md` records that state.

Known logic-in-controller (drift risk once an API is added): `ConversationController` parses
recipients, builds the signature, computes the conversation key and picks the sender mailbox
inline; these must move into services before a second (API) caller is added.
