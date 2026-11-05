# BACKEND.md — architecture (reverse-engineered 2026-11-05)

Spring Boot 3 / Java 21 monolith ("ConexusMail"). Server-rendered Thymeleaf web
app; there is **no REST/JSON API yet** (see `API.md`).

## Deviations from the generic target stack
- Persistence is **Spring Data JPA/Hibernate** (`ddl-auto: validate`), **not jOOQ**. No codegen step.
- Flyway migrations: `src/main/resources/db/migration/V1..V35` (immutable once merged).
- Postgres 16 prod/local, H2 in tests. Dockerfile/docker-compose exist in the tree even though CLAUDE.md says Docker was dropped (TBD: stale files).

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `auth` | Form login + Google OIDC, registration, password reset, email verification, login throttling, account settings |
| `billing` | Stripe checkout/portal/webhook, plans (`FREE/PRO/BUSINESS`), `PlanLimitService` limits, trial nudges |
| `email` | IMAP import/polling/IDLE, Gmail OAuth, MIME parsing, credential encryption, `ConversationKeyService` backfill |
| `service` | IM transform, `ChatService`/`ConversationService`, reply sending, body cleaning, view-model records |
| `web` | Thymeleaf controllers (chats, mailboxes, saved searches, pricing, legal, PWA, SEO), SSE `ChatEventStream` |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `team`, `digest`, `admin`, `activation`, `reengagement` | Teams+invites, digests, admin metrics, schedulers |

## Domain
`User` 1—* `MailAccount` (IMAP password or Gmail OAuth, encrypted creds) ;
`EmailThread` 1—* `Message` (`Participant` sender, `MessageRecipient`s, `Attachment`s);
chats are keyed by a participant-set `conversationKey` and merge threads into one timeline.
Also: `Subscription`, `PlanChangeEvent`, `Team`/`TeamMember`/`TeamInvite`, `SavedSearch`,
`DigestEmailPreference`, `AuthEvent`, password-reset / email-verification tokens, `persistent_logins`.

## Auth model
- Session cookie (form login at `/login`, Google OIDC when `GOOGLE_CLIENT_ID/SECRET` set), remember-me via `persistent_logins`.
- CSRF is **enabled** except `/h2-console/**` and `/billing/webhook`.
- Everything not in the `permitAll` list in `SecurityConfig` requires authentication.
- No token (JWT/API key) auth exists → a mobile client cannot authenticate today.

## Key flows
- Import: `MailboxPollingService` (scheduled + on-demand) → `ImapClient` → `EmailImportService` → `MimeMessageParser` → threads by Message-ID/In-Reply-To/References → `conversationKey`.
- Live updates: `ImapIdleService` (paid tiers) → `NewMailEvent` → `ChatEventStream` → SSE `/chats/events`.
- Send: `ReplyService` / `OutboundMessageService` via SMTP.

## Web ↔ API relationship
Controllers delegate to services (`ChatService`, `ConversationListService`, `ReplyService`, `MailAccountService`, …). A future REST layer must reuse these services; do not copy controller logic.
