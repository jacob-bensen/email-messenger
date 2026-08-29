# Backend architecture

This document is a snapshot of `email-messenger` as it exists in the code
today. Prefer this over `CLAUDE.md` for architectural detail; `CLAUDE.md`
is the routine's roadmap.

## Runtime

Spring Boot 3 on Java 21, packaged with Maven (wrapper).

- Entrypoint: `com.emailmessenger.EmailMessengerApplication`.
- Views: server-rendered Thymeleaf. There is no SPA; the browser client
  is HTML + a small amount of vanilla JS + a service worker (PWA shell).
- Persistence: Spring Data JPA over PostgreSQL 16 (H2 for tests). Schema
  is owned by Flyway migrations in `src/main/resources/db/migration`
  (currently V1–V35).
- Auth: form login + Google OIDC (`spring-boot-starter-security`
  configuration in `com.emailmessenger.auth`).
- Billing: Stripe subscriptions via `com.emailmessenger.billing`.
- Mail I/O: Jakarta Mail for IMAP polling / IMAP IDLE (paid tier) and
  SMTP send.

## Package layout (actual)

```
com.emailmessenger
├── EmailMessengerApplication  bootstrap
├── activation                 onboarding nudge / activation follow-ups
├── admin                      operator dashboard, revenue metrics
├── auth                       accounts, sign-in, Google OIDC, email verification, password reset
├── billing                    Stripe checkout, portal, webhook, plan changes
├── digest                     periodic digest email (opt-out endpoint under /digest/opt-out)
├── domain                     JPA entities
├── email                      IMAP polling & IDLE, SMTP send, mailer-daemon handling
├── reengagement               win-back / trial-end / lastchance email jobs
├── repository                 Spring Data repositories
├── service                    conversation grouping, IM transform, reply pipeline
├── team                       team + invite domain and controller
└── web                        Thymeleaf controllers, view models, PWA / SEO endpoints
```

## Core domain

Threading (`service/`):
- `Message` (RFC 822) rows carry `messageId`, `inReplyTo`, `references`.
- `EmailThread` groups messages by header chain + a `conversationKey`
  (V32) that maps a participant-set to a stable "chat" identity, so the
  `/chats` workspace can render messages from many threads as one
  conversation.
- `IMTransformService` strips quoted history and collapses same-sender
  runs into `BubbleRun`s for the IM view.
- `ReplyService` composes and sends outbound replies via SMTP, threading
  onto the last message of the target thread (see roadmap: per-bubble
  reply is still open).

Accounts & mailboxes:
- `User` owns one or more `MailAccount`s.
- `MailAccount.authType` is `PASSWORD` (IMAP+SMTP) or `OAUTH_GOOGLE`
  (Gmail OAuth). Password-mode credentials are encrypted at rest with
  `MAILBOX_ENCRYPTION_*`.
- Polling cursors: `last_seen_uid` (inbox) and `last_seen_sent_uid`
  (sent folder, added V31).
- Signatures live on `MailAccount` (V35).

Billing:
- `Subscription` mirrors the Stripe subscription for a user, with plan
  (Free / Pro / Business), status, current period end (V17) and
  cancellation reason (V27).
- `PlanChangeEvent` (V26) is the append-only ledger of upgrades /
  downgrades for revenue analytics.
- Only the four Stripe events listed in the README are honored.

Teams: `Team`, `TeamMember`, `TeamInvite` (V24) — invited via
`/team/invite`, accepted via `/team/invite/accept`.

Saved searches: `SavedSearch` — user-defined filters on the /chats rail.

## Real-time delivery

`ChatEventStream` publishes `NewMailEvent`s to per-user Server-Sent
Events subscriptions at `GET /chats/events`. `MailboxPollingService`
publishes when new mail lands from a poll; `ImapIdleService` (paid tier,
`mailbox.idle.enabled`) publishes from an IDLE channel. The browser
client subscribes and morphs the conversation rail + open pane in place
via `templates/fragments/live-refresh.html` and the /chats/{key}/pane
fragment endpoint.

## What is NOT here yet

- No REST API for third-party or mobile clients — the HTTP surface is
  Thymeleaf pages and form posts (see `docs/API.md`). The one JSON-ish
  endpoint is the SSE stream.
- No blob store for attachments — `Attachment.blob_ref` is always null;
  attachment download is not wired up.
- No Web Push — the PWA has a service worker, but push subscriptions,
  VAPID keys, and notification opt-in are all open roadmap items.
- No mobile client — the sibling `conexus-mail-mobile` repo is empty
  (README + .gitignore only) and no product design or API contract has
  been agreed. See that repo's `INTERNAL_TODO.md`.
