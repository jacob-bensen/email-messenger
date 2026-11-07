# Backend architecture

Repo classification: **BACKEND** (pom.xml, Spring Boot, Flyway migrations
V1–V35). Sibling: `conexus-mail-mobile` (FRONTEND, currently only a README).

## Deviations from the generic target stack

- Persistence is **Spring Data JPA/Hibernate**, not jOOQ. No codegen step.
- Build is Maven wrapper (`./mvnw`); Java 21; PostgreSQL 16 (H2 in tests).
- Not deployed via Docker/CI per `CLAUDE.md`, though `Dockerfile`,
  `docker-compose.yml` and `DEPLOY.md` still exist in the repo (stale, see
  `INTERNAL_TODO.md`).

## Packages (`com.emailmessenger`)

| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, pricing, legal, PWA, SEO), `ConversationListService`, `ChatService`, `OutboundMessageService`, SSE (`ChatEventStream`) |
| `service` | IM transform: `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`, view records (`ChatConversation`, `BubbleMessage`, …) |
| `domain` | JPA entities (User, MailAccount, EmailThread, Message, Participant, Attachment, Subscription, Team*, SavedSearch, …) |
| `repository` | Spring Data repositories |
| `email` | IMAP/Gmail integration: import, polling, IDLE, OAuth, credential encryption |
| `auth` | Security config, registration, Google OIDC, password reset, email verification, account settings |
| `billing` | Stripe checkout/portal/webhook, plans (`FREE/PRO/BUSINESS`), `PlanLimitService` |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digests, lifecycle emails, admin metrics |

## Domain model

`User` 1—* `MailAccount` (IMAP password or Gmail OAuth; credentials encrypted
by `CredentialEncryptor`). Mail imports into `EmailThread` (header-based
threading) → `Message` (+ `MessageRecipient`, `Attachment`) ↔ `Participant`.
A **conversation** is a participant set identified by `conversationKey`
(`ConversationKeyService`), merging many threads into one texting timeline.

## Schema overview (Flyway, latest V35)

Tables: users, persistent_logins, mail_accounts, participants, email_threads,
messages, message_recipients, attachments, thread_notes, subscriptions,
plan_change_events, teams, team_members, team_invites, saved_searches,
digest_email_preferences, password_reset_tokens, email_verifications,
auth_events. Migrations are immutable once merged.

## Auth model

Session cookie + CSRF (form login, Google OIDC), remember-me tokens in DB.
No bearer/API auth yet. See `API.md` §2.

## Web ↔ API relationship

There is no REST API. Controllers in `web` call services directly; any future
`/api/v1` controllers must call the **same** services (`ConversationListService`,
`ChatService`, `OutboundMessageService`, …). Known logic living in controllers
that must move into services first: see `INTERNAL_TODO.md`.
