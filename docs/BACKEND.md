# BACKEND.md — architecture

Reverse-engineered from the code on 2026-10-15. Unknowns are marked TBD.

## Classification
This repo is the BACKEND: `pom.xml` (Spring Boot, Java 21), Flyway migrations
(`V1`–`V35`), Spring MVC + Thymeleaf. Sibling repo `conexus-mail-mobile` is the
FRONTEND (Flutter; currently only a README).

**Deviation from the generic routine:** persistence is Spring Data **JPA/Hibernate**,
not jOOQ. There is no jOOQ codegen. Follow the existing JPA conventions in `CLAUDE.md`.

## Stack
Java 21, Spring Boot 3, Spring Security, Spring Data JPA, Flyway, PostgreSQL 16
(H2 in tests), Thymeleaf, Jakarta Mail (IMAP/SMTP), Stripe, Maven wrapper.

## Package layout (`com.emailmessenger`)
| Package | Role |
|---|---|
| `domain` | JPA entities: `User`, `MailAccount` (`AuthType` PASSWORD/XOAUTH2), `EmailThread`, `Message`, `MessageRecipient`, `Participant`, `Attachment`, `Subscription`, `Plan` (FREE/PRO/BUSINESS), `Team*`, `SavedSearch`, auth/token entities |
| `repository` | Spring Data repositories |
| `email` | IMAP import/polling/IDLE (`MailboxPollingService`, `ImapIdleService`, `EmailImportService`, `MimeMessageParser`), Gmail OAuth, credential encryption |
| `service` | Conversation logic: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`, view records (`ChatConversation`, `ConversationListItem`, `ChatMember`, `BubbleMessage`) |
| `web` | Thymeleaf controllers + view services (`ConversationController`, `ChatService`, `ConversationListService`, `OutboundMessageService`, `MailboxController`, SSE `ChatEventStream`, PWA, SEO, legal) |
| `auth` | Security config, registration/login, Google OIDC, password reset, email verification, account settings, login throttling/audit |
| `billing` | Stripe checkout/portal/webhook, `PlanLimitService`, trial nudges |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, weekly digests, lifecycle emails, admin metrics |

## Schema overview
Flyway, `src/main/resources/db/migration`, `V1`..`V35`. Tables (current): `users`,
`mail_accounts`, `email_threads`, `messages`, `message_recipients`, `participants`,
`attachments` (`blob_ref` always null — bytes not stored), `subscriptions`,
`plan_change_events`, `teams`, `team_members`, `team_invites`, `saved_searches`,
`digest_email_preferences`, `auth_events`, `password_reset_tokens`,
`email_verifications`, `persistent_logins`. (`thread_notes` dropped in V33.)
Migrations are immutable once merged.

## Core concept: conversationKey
A conversation is keyed by the participant set (`ConversationKeyService`), not by
email thread. One conversation merges many `EmailThread`s; the chat view groups
messages into sender bubble runs. Owner-scoped: every lookup takes the `User`
and an optional mailbox scope address.

## Auth model
- Session-cookie auth (Spring Security): form login at `POST /login`
  (`email`, `password`, optional `remember-me`; 30-day persistent token), optional
  Google OIDC (only when `GOOGLE_CLIENT_ID` is configured), `POST /logout`.
- **CSRF is enabled** (exempt: `/h2-console/**`, `/billing/webhook`).
- Login throttling (`LoginThrottleFilter`), auth event audit.
- Public routes are whitelisted in `SecurityConfig`; everything else requires login.
- **There is no token/Bearer auth and no `/api/**` namespace.** (TBD for mobile — see INTERNAL_TODO.)

## Web vs REST
The only `@RestController` is `BillingWebhookController` (`POST /billing/webhook`,
Stripe). Everything else is a Thymeleaf `@Controller` returning HTML/redirects, with
two JSON-less exceptions: SSE stream `/chats/events` and `/chats/{key}/pane` HTML
fragment. Business logic lives in `service`/`web`/`email`/`billing` services that
controllers call; a mobile API must reuse those services (see `docs/API.md`).

## Background jobs
`MailboxPollingScheduler` (polling, feature flag), `ImapIdleService` (paid tiers,
`mailbox.idle.enabled`), weekly digest, activation, re-engagement, trial-end
conversion, admin weekly digest schedulers.
