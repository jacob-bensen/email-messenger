# BACKEND.md — architecture

Reverse-engineered from the code on 2026-11-07. Anything not verified is marked **TBD**.

## Stack
Java 21, Spring Boot 3, Spring Data **JPA/Hibernate** (not jOOQ — there is no jOOQ in `pom.xml`),
PostgreSQL 16 (H2 in tests), Flyway (`src/main/resources/db/migration`, V1–V35), Thymeleaf,
Spring Security (form login + optional Google OIDC), Stripe, Jakarta Mail (IMAP/SMTP), GreenMail +
Testcontainers in tests. `ddl-auto: validate`; `open-in-view: false`.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `domain` | JPA entities: `User`, `MailAccount`, `EmailThread`, `Message`, `MessageRecipient`, `Participant`, `Attachment`, `Subscription`, `Plan`, `Team*`, `SavedSearch`, token/audit entities |
| `repository` | Spring Data repositories |
| `email` | IMAP/SMTP integration: `MimeMessageParser`, `EmailImportService`, `MailboxPollingService` (+scheduler), `ImapIdleService` (paid tiers), `GmailOAuthClient`, `CredentialEncryptor`, `MailAccountService` |
| `service` | IM transform: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`; view records `ChatConversation`, `BubbleRun`, `BubbleMessage`, `ChatMember`, `ConversationListItem` |
| `web` | MVC controllers + web-side services (`ChatService`, `ConversationListService`, `OutboundMessageService`, `OnboardingService`, SSE `ChatEventStream`), PWA/SEO controllers |
| `auth` | Security config, registration, login throttle, password reset, email verification, Google OIDC, account settings |
| `billing` | Stripe checkout/portal/webhook, `PlanLimits`/`PlanLimitService` |
| `team`, `digest`, `reengagement`, `activation`, `admin` | teams/invites, digest emails, lifecycle emails, admin metrics (`/admin/**`, email allowlist) |

## Core model
- A **conversation** is keyed by `conversationKey` (participant-set hash, V32) — one chat per
  set of participants, spanning several `EmailThread`s (Message-ID / In-Reply-To / References).
- `ChatService.buildFor(owner, [mailbox], key)` → `ChatConversation` (members + sender-grouped bubble runs, quoted text stripped).
- Sending: `ReplyService` (SMTP; reply keeps thread headers, new email otherwise) then
  `OutboundMessageService.record*` persists the outbound `Message` (V30).
- Plan caps (`PlanLimits`): FREE 3 mailboxes / 500 threads, PRO 5 / unlimited, BUSINESS unlimited. Saved searches unlimited.

## Auth model
- Session cookie (`JSESSIONID`) + persistent remember-me (30 days, token repo) via form login at
  `POST /login` (`email`, `password`); optional Google OIDC (`/auth/google/start`).
- **CSRF is ON** for everything except `/h2-console/**` and `/billing/webhook`.
- Public routes are whitelisted in `SecurityConfig`; everything else is `authenticated`.
- **No token/bearer auth exists.** A mobile client cannot authenticate today (see INTERNAL_TODO).

## Web ↔ API relationship
There is **no REST API**. The only JSON-ish endpoints are `POST /billing/webhook` (Stripe) and
`GET /chats/events` (SSE). All user features are server-rendered form/redirect flows. See `docs/WEB.md`.
Plan: `/api/v1/**` (see `docs/API.md` §Planned) must call the same services, which requires extracting
logic currently inlined in `ConversationController` (INTERNAL_TODO #2).

## Branching note
The routine prompt says "work on master"; the sessions that run it are pinned to feature branches and
the backend CLAUDE.md forbids pushing to main, so work lands on the session branch.
