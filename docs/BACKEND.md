# BACKEND.md — architecture

Repo classification: **BACKEND** (Spring Boot 3, Java 21, Maven, Flyway V1–V35, Thymeleaf).
Deviation from the routine's assumed stack: persistence is **Spring Data JPA/Hibernate**
(`ddl-auto: validate`), not jOOQ. No codegen step exists.

## Modules (`com.emailmessenger.*`)
| Package | Role |
|---|---|
| `auth` | Form login (`email`/`password`), Google OIDC (only when `GOOGLE_CLIENT_ID` set), remember-me (30d), login throttle, email verification, password reset, `/account/*` settings. `SecurityConfig`. |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimits`: FREE 3 mailboxes/500 threads; PRO 5; BUSINESS unlimited), trial nudges. |
| `domain` | JPA entities (tables: users, subscriptions, plan_change_events, mail_accounts, email_threads, messages, message_recipients, participants, attachments, saved_searches, teams, team_members, team_invites, auth_events, digest_email_preferences, password_reset_tokens, email_verifications). |
| `repository` | Spring Data repositories. |
| `email` | IMAP/SMTP: import (`EmailImportService`, `MimeMessageParser`), polling (`MailboxPollingService/Scheduler`), IMAP IDLE (paid tiers), Gmail OAuth, credential encryption. |
| `service` | IM transform: `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`, view records (`BubbleRun`, `ChatConversation`…). |
| `web` | Thymeleaf controllers (/chats, /mailboxes, /searches, pricing, PWA, SEO), `ChatEventStream` (SSE), `GlobalExceptionHandler`. |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digests, lifecycle emails, operator `/admin/revenue` metrics (allowlist `admin.emails`). |

## Auth model
Session cookie (JSESSIONID) + CSRF on all POSTs (exempt: `/billing/webhook`, `/h2-console/**`).
Everything not on the permitAll list requires authentication. **There is no token/bearer auth**;
a mobile client cannot authenticate today (see INTERNAL_TODO.md).

## Web vs REST
All user-visible behavior is server-rendered HTML via controllers that call services
(`ChatService`, `ConversationService`, `MailAccountService`, …). There is no REST layer. Only
JSON/SSE-ish endpoints: `POST /chats/refresh`, `GET /chats/events` (SSE), `POST /billing/webhook`,
`/manifest.webmanifest`. See API.md.

## Config
Env vars: `JDBC_DATABASE_URL/USERNAME/PASSWORD`, `MAIL_*`, `STRIPE_*`, `GOOGLE_CLIENT_*`,
`MAILBOX_ENCRYPTION_PASSWORD/SALT`, `MAILBOX_POLLING_ENABLED`, `MAILBOX_IDLE_ENABLED`. Tests use H2.
Operator planning docs live in `master/` (PLAN, BACKLOG, CHANGELOG, MASTER_ACTIONS) — a separate,
older routine's artifacts; root `CHANGELOG.md` is this sync routine's.
