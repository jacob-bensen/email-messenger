# BACKEND.md — architecture (reverse-engineered 2026-10-31)

Repo classification: **BACKEND** (pom.xml + Spring Boot + Flyway `db/migration/V1..V35`).
Sibling: `conexus-mail-mobile` (FRONTEND, Flutter; currently only a README).

> Deviation from the generic target stack: persistence is **Spring Data JPA**, not jOOQ.
> There is no jOOQ codegen. Java 21, Spring Boot 3, Maven wrapper, PostgreSQL 16 (H2 in tests).

## Package layout (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web/` | Thymeleaf controllers (chats, mailboxes, saved searches, pricing, PWA, SEO, legal), `ChatEventStream` (SSE), list/chat services |
| `auth/` | Spring Security config, form login + Google OIDC, registration, password reset, email verification, account settings, login throttle |
| `billing/` | Stripe checkout/portal/webhook, plan limits |
| `team/` | team invites/membership |
| `digest/` | digest emails + opt-out |
| `admin/` | revenue/retention metrics (`/admin/**`) |
| `email/` | IMAP polling, IMAP IDLE, Gmail OAuth, SMTP send, import/threading |
| `service/` | IM transform (`IMTransformService`, `EmailBodyCleaner`), `ConversationService`, `ConversationKeyService`, `ReplyService`, view-model records |
| `domain/` | JPA entities (User, MailAccount, EmailThread, Message, Participant, Attachment, Subscription, Team*, SavedSearch, ...) |
| `repository/` | Spring Data repositories |

## Schema overview (Flyway)
users, persistent_logins, auth_events, password_reset_tokens, email_verifications,
subscriptions, plan_change_events, mail_accounts, participants, email_threads, messages,
message_recipients, attachments (`blob_ref` always null today), saved_searches, teams,
team_members, team_invites, digest_email_preferences. Latest migration: `V35__mail_account_signature.sql`.
Migrations are immutable; add a new `V{n}`.

## Auth model
- Session cookie (`JSESSIONID`) + persistent remember-me (30d, JDBC token store).
- Form login at `POST /login` (params `email`, `password`); Google OIDC when `GOOGLE_CLIENT_ID` set.
- CSRF **enabled**, only `/billing/webhook` and `/h2-console/**` exempt.
- Everything not on the permit list in `SecurityConfig` requires authentication.
- **No token/bearer auth exists.** A mobile client cannot authenticate today (see INTERNAL_TODO).

## Web vs REST
There is currently **no REST/JSON API**. All user-facing behaviour is server-rendered
Thymeleaf plus one SSE endpoint (`/chats/events`) and the Stripe webhook. Controllers
call services (`ConversationListService`, `ConversationService`, `ReplyService`,
`MailAccountService`, ...); a future `/api/v1` layer must reuse these services
(do not parse the Model-building logic out of the Thymeleaf controllers — extract it).
