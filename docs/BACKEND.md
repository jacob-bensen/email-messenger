# Backend architecture

`email-messenger` is the ConexusMail backend: a Spring Boot 3 / Java 21
application that serves the marketing site, the SaaS web app, and all
persistence. There is currently **no REST API for a mobile client** — the
UI is entirely server-rendered Thymeleaf. See `API.md` for status.

## Runtime

- Java 21, Spring Boot 3.x
- PostgreSQL 16 (H2 in tests), Flyway migrations under
  `src/main/resources/db/migration`
- Jakarta Mail for IMAP / SMTP; IMAP IDLE for paid tiers
- Thymeleaf 3 templates under `src/main/resources/templates`
- Server-sent events (SSE) at `/chats/events` for in-page live updates
- Stripe billing (Free / Pro / Business) with a signed webhook at
  `/billing/webhook`

## Package layout (`com.emailmessenger`)

| Package        | Responsibility                                              |
| -------------- | ----------------------------------------------------------- |
| `web`          | Thymeleaf controllers, form/view models, redirect handlers  |
| `service`      | Conversation grouping, IM transform, quoted-reply stripping |
| `domain`       | JPA entities (`Message`, `EmailThread`, `Participant`, …)   |
| `repository`   | Spring Data JPA repositories                                |
| `email`        | IMAP/SMTP integration, mail-account state, polling & IDLE   |
| `auth`         | Form login + Google OIDC, sessions, password reset, verify  |
| `billing`      | Stripe checkout, portal, webhook, plan limits, banners      |
| `admin`        | Operator dashboards (revenue, churn, funnel, win-back)      |
| `team`         | Teams, invites, member roles                                |
| `activation`   | New-user activation nudges + scheduler                      |
| `digest`       | Weekly digest emails                                        |
| `reengagement` | Win-back / at-risk retention outreach                       |

## Data model highlights

Domain entities live in `com.emailmessenger.domain`:

- `User`, `Subscription`, `Plan`, `PlanChangeEvent`, `AuthEvent`,
  `PasswordResetToken`, `EmailVerificationToken`
- `MailAccount` (IMAP password or Gmail OAuth), `AuthType`,
  `DigestEmailPreference`
- `EmailThread`, `Message`, `MessageRecipient`, `Participant`,
  `Attachment`, `RecipientType`
- `Team`, `TeamMember`, `TeamMemberRole`, `TeamInvite`
- `SavedSearch`, `CancellationReason`

Conversations are grouped in the service layer by a normalized
`conversationKey` (participant-set), not by RFC-822 threading alone; see
`service/ConversationKeyService.java`.

## Live delivery

- `email/MailboxPollingService` publishes `NewMailEvent` after import.
- `web/ChatEventStream` fans events out over SSE at `/chats/events`.
- `chat.html` + `fragments/live-refresh.html` do in-place morphs on
  receipt (no full-page reload).
- Paid-tier accounts additionally use `email/ImapIdleService` for
  push-style delivery on the IMAP side.

## Auth model

- Form login (`/login`) with password hashing via Spring Security.
- Google OIDC login (`/auth/google/start` → `/login/oauth2/code/google`).
- Session cookies; CSRF enabled for state-changing endpoints.
- Admin endpoints (`/admin/**`) gated by an email allowlist configured
  in `application.yml` under `admin.emails`.

## Website ↔ API relationship

Every current entry point (`web/`, `auth/`, `billing/`, `team/`,
`digest/`, `admin/`) is a Thymeleaf controller returning a view name.
Business logic lives in service classes and is called from these
controllers. If a REST API is added for the mobile client, it must reuse
those services and never fork logic into a parallel path (see
`API.md`).
