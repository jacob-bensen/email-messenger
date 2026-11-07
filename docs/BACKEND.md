# BACKEND.md — architecture

Reverse-engineered 2026-11-07 from the code. Unknowns are marked TBD.

## Stack (actual)
Java 21, Spring Boot 3, Spring Data **JPA** (not jOOQ — no codegen step), Flyway
(`src/main/resources/db/migration`, V1–V35), PostgreSQL (prod/local), H2 (tests),
Thymeleaf, Spring Security, Stripe, Jakarta Mail (IMAP/SMTP), Gmail OAuth.

## Modules (`com.emailmessenger.*`)
| Package | Role |
|---|---|
| `auth` | Registration, form login + remember-me, Google OIDC, password reset, email verification, account settings, login throttling, auth audit events |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), billing banners, trial-conversion jobs |
| `email` | Mailbox connect (IMAP password, Gmail OAuth), import (`EmailImportService`), polling (`MailboxPollingService`), IMAP IDLE, MIME parsing |
| `service` | Conversation model: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ReplyService`, conversation keys |
| `web` | Thymeleaf controllers for chats, mailboxes, pricing/landing, legal, PWA, SEO, SSE stream (`/chats/events`) |
| `team`, `digest`, `admin`, `activation`, `reengagement` | Teams/invites, digests, operator metrics, lifecycle emails |
| `domain`, `repository` | JPA entities / Spring Data repositories |

## Schema overview
`users`, `subscriptions`, `plan_change_events`, `mail_accounts` (auth type, polling policy,
last-seen UIDs, signature), `email_threads` (owner, unread, `conversation_key`), `messages`
(+ outbound flag), `message_recipients`, `participants`, `attachments` (`blob_ref` always null today),
`saved_searches`, `digest_email_preferences`, `teams`/`team_members`/`team_invites`,
`password_reset_tokens`, `email_verifications`, `auth_events`, `persistent_logins` (remember-me).

## Auth model
Session cookie (`JSESSIONID`) via form login (`POST /login`, params `email`, `password`) or Google OIDC
(only when `GOOGLE_CLIENT_ID` is set); 30-day remember-me cookie. **CSRF is enabled** (exempt:
`/h2-console/**`, `/billing/webhook`). Everything not on the permit-all list in `SecurityConfig` requires login.
There is **no token-based (bearer/JWT) auth** — TBD for mobile.

## Web vs REST
There is **no REST API yet**. The only `@RestController` is the Stripe webhook. All user-facing
behavior is server-rendered Thymeleaf; controllers call services (`ConversationListService`,
`ReplyService`, `MailAccountService`, …). See `WEB.md`, `API.md`.
