# Backend architecture

Reverse-engineered 2026-09-30 from the code. Unknowns are marked TBD.

## Stack (actual)
Java 21, Spring Boot 3, Spring Data **JPA/Hibernate** (`ddl-auto: validate`; **not jOOQ**), PostgreSQL 16
(H2 in tests), Flyway (35 migrations, `V1`..`V35`), Thymeleaf, Spring Security, Stripe (`stripe-java`),
Jakarta Mail (IMAP/SMTP), jsoup. Maven wrapper.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats, mailboxes, saved searches, pricing, legal, PWA, SEO), SSE stream, view services |
| `auth` | Security config, form + Google OIDC login, registration, password reset, email verification, login throttle, account pages |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimits`), trial nudges |
| `email` | IMAP import/polling/IDLE, MIME parsing, Gmail OAuth, credential encryption |
| `service` | Conversation building, IM transform, body cleaning, reply sending, conversationKey |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digests, lifecycle emails, admin metrics |
| `domain` / `repository` | JPA entities / Spring Data repositories |

## Data model (overview)
`User`, `Subscription` (plan FREE/PRO/BUSINESS), `MailAccount` (IMAP password or Gmail OAuth; credentials
encrypted by `CredentialEncryptor`), `EmailThread` (owner + `conversationKey` = participant-set key),
`Message`, `MessageRecipient`, `Participant`, `Attachment` (`blob_ref` always null: bytes not stored),
`SavedSearch`, `Team`/`TeamMember`/`TeamInvite`, `DigestEmailPreference`, `AuthEvent`,
`PasswordResetToken`, `EmailVerificationToken`, `PlanChangeEvent`. Schema source of truth: `src/main/resources/db/migration`.

## Auth model
Session cookie (`JSESSIONID`) + 30-day persistent remember-me (JDBC token repo). Form login at `/login`
(param `email`, `password`); optional Google OIDC (enabled only when `GOOGLE_CLIENT_ID` is set). CSRF is
**on** everywhere except `/h2-console/**` and `/billing/webhook`. Everything not on the permitAll list requires
authentication. `/admin/**` is restricted to `admin.emails` (non-admins get 404). There is **no token/API-key auth**,
so a mobile client cannot authenticate today (see `INTERNAL_TODO.md`).

## Web vs REST
There is no REST API. The only JSON/SSE responses are `POST /chats/refresh` (JSON) and `GET /chats/events` (SSE),
both session-authenticated and consumed by the website's JS. See `API.md`, `WEB.md`.

## Background jobs
`MailboxPollingScheduler` (on by default), `ImapIdleService` (paid tiers, `mailbox.idle.enabled`), and cron
schedulers (digest, reengagement, activation, trial-end, admin weekly digest) that stay off unless their
`*.enabled` flag is true.
