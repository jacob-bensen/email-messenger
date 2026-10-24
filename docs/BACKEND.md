# Backend architecture

Spring Boot 3 / Java 21 / Maven. **Persistence is Spring Data JPA (Hibernate) + Flyway,
not jOOQ** (differs from the generic routine assumption). PostgreSQL in prod/local, H2 in tests.

## Packages (`com.emailmessenger`)
- `auth` – form + Google OIDC login, registration, password reset, email verification, login throttle, account settings.
- `billing` – Stripe checkout/portal/webhook, plans & limits (`PlanLimitService`), trial nudges.
- `email` – IMAP/Gmail-OAuth mailbox connect, polling (`MailboxPollingService`), IMAP IDLE, MIME parsing, import.
- `service` – IM transform (`IMTransformService`, `EmailBodyCleaner`), conversations, replies, conversation keys.
- `web` – Thymeleaf controllers for chats, mailboxes, saved searches, pricing/landing, PWA, SEO, SSE (`ChatEventStream`).
- `team`, `digest`, `activation`, `reengagement`, `admin` – teams/invites, digests, lifecycle emails, operator metrics.
- `domain` / `repository` – JPA entities / Spring Data repos.

## Schema
Flyway migrations `V1`..`V35` in `src/main/resources/db/migration` (immutable once merged).
Tables: users, subscriptions, plan_change_events, mail_accounts, email_threads, messages,
message_recipients, participants, attachments (`blob_ref` always null today), saved_searches,
thread_notes, teams, team_members, team_invites, digest_email_preferences, auth_events,
email_verifications, password_reset_tokens, persistent_logins.

## Auth model
Session cookies + CSRF; see `API.md`. Public routes are the allowlist in `SecurityConfig`.

## Web vs API
Only the Thymeleaf web layer exists; there is no REST layer. See `API.md` for the plan.

Other planning docs: `master/PLAN.md`, `master/BACKLOG.md`, `master/CHANGELOG.md` (product log), `CLAUDE.md` roadmap.
