# BACKEND.md — architecture

Repo classification: **BACKEND** (pom.xml, Spring Boot, Flyway migrations, Thymeleaf).
Sibling: `jacob-bensen/conexus-mail-mobile` (FRONTEND, currently an empty scaffold).

> Deviation from the generic target stack: persistence is **Spring Data JPA /
> Hibernate** (`ddl-auto: validate`), not jOOQ. There is no jOOQ codegen step.
> Schema is owned by Flyway (`src/main/resources/db/migration`, V1…, 35 files).

## Stack
Java 21, Spring Boot 3 (web, thymeleaf, data-jpa, validation, mail, security,
oauth2-client, actuator), Flyway, PostgreSQL 16 (prod/local) / H2 (tests),
Jakarta Mail (IMAP/SMTP), Stripe Java, jsoup. Tests: JUnit 5, Spring Boot Test,
Testcontainers, GreenMail, spring-security-test.

## Package layout (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats, mailboxes, saved searches, pricing/landing, legal, PWA, SEO), `ChatEventStream` (SSE) |
| `auth` | Spring Security config, form login + Google OIDC, registration, verification, password reset, account settings, login throttle/audit |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), trial nudges |
| `email` | IMAP polling (`MailboxPollingService`), IDLE (`ImapIdleService`), import (`EmailImportService`), MIME parsing, Gmail OAuth, credential encryption |
| `service` | Conversation building / IM transform (`IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`) |
| `domain` | JPA entities |
| `repository` | Spring Data repositories |
| `team`, `digest`, `activation`, `reengagement`, `admin` | teams/invites, digests, lifecycle emails, `/admin/revenue` metrics |

## Domain / schema overview
Tables: `users`, `subscriptions`, `plan_change_events`, `mail_accounts`,
`email_threads`, `messages`, `message_recipients`, `participants`,
`attachments` (`blob_ref` always null today), `thread_notes`, `saved_searches`,
`teams`, `team_members`, `team_invites`, `digest_email_preferences`,
`email_verifications`, `password_reset_tokens`, `auth_events`,
`persistent_logins` (remember-me). Conversations in /chats are keyed by a
participant-set `conversationKey` (not by a single thread id).

## Auth model
Server-side **session cookie** (form login `POST /login`, param `email`/`password`;
optional Google OIDC, active only when `GOOGLE_CLIENT_ID/SECRET` are set) plus a
30-day persistent remember-me cookie. **CSRF is enabled** (exempt: `/billing/webhook`,
`/h2-console/**`). Everything not in the `permitAll` list in
`auth/SecurityConfig.java` requires authentication. **There is no token/JWT auth
and no `/api/**` surface** — see API.md.

## Web vs REST
All HTTP endpoints are server-rendered MVC (`@Controller` returning view names or
redirects). The only `@RestController` is the Stripe webhook. Controllers delegate
to services (`ChatService`, `ReplyService`, `MailAccountService`, `BillingService`,
…) and a few inject repositories directly (see INTERNAL_TODO). A future mobile API
must reuse these services rather than duplicate logic.

## Live updates
`MailboxPollingService` / `ImapIdleService` publish `NewMailEvent`; `ChatEventStream`
fans out to per-user SSE emitters at `GET /chats/events`.

## Where the older planning docs live
`master/` (a directory committed on the branch) holds `PLAN.md`, `BACKLOG.md`,
`MASTER_ACTIONS.md` and a long `CHANGELOG.md` from earlier routines. Root
`CLAUDE.md` holds the active roadmap.
