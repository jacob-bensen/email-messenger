# Backend architecture

Reverse-engineered from the code on 2026-10-27. Items not verifiable from code are marked **TBD**.

## Stack (actual)

Java 21, Spring Boot 3 (web, thymeleaf, data-jpa, validation, mail, security, oauth2-client, actuator), **JPA/Hibernate** (not jOOQ — there is no jOOQ dependency or codegen), Flyway (35 migrations, `V1`–`V35`), PostgreSQL 16 (H2 in tests), Jakarta Mail (IMAP/SMTP), Stripe Java, jsoup. Maven wrapper (`./mvnw`).

## Packages (`com.emailmessenger`)

| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats workspace, mailboxes, pricing/landing, legal, PWA, SEO), `ChatService`, `ConversationListService`, `OutboundMessageService`, SSE `ChatEventStream` |
| `auth` | Form login + Google OIDC, registration, email verification, password reset, account settings, login throttling, auth-event audit |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), banners, trial nudges |
| `email` | Mailbox connect (IMAP password + Gmail OAuth), import (`EmailImportService`, `MimeMessageParser`), polling + IMAP IDLE, credential encryption |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), `ConversationService`, `ConversationKeyService`, `ReplyService`, demo data |
| `domain` / `repository` | JPA entities / Spring Data repositories |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams + invites, digests, lifecycle emails, operator metrics (`/admin/revenue`) |

## Data model (tables)

`users`, `persistent_logins`, `participants`, `email_threads` (has `conversation_key`, owner, unread), `messages` (outbound flag), `message_recipients`, `attachments` (`blob_ref` always null today), `mail_accounts` (auth type, polling policy, last-seen UIDs, signature), `subscriptions`, `plan_change_events`, `saved_searches`, `digest_email_preferences`, `teams`, `team_members`, `team_invites`, `auth_events`, `email_verifications`, `password_reset_tokens`. A conversation is identified by the participant-set `conversationKey` (string), spanning many threads.

## Auth model

Server-side session (`JSESSIONID`) + 30-day persistent remember-me (JDBC token repo). Form login at `POST /login` (params `email`, `password`), optional Google OIDC. **CSRF is enabled** for everything except `/billing/webhook` and `/h2-console/**`. Everything not on the permit-list in `SecurityConfig` requires authentication. There is **no token/API-key auth**, so a native client cannot authenticate today (see `INTERNAL_TODO.md`).

## Web vs API

All routes today are server-rendered Thymeleaf (`@Controller`) except:
- `POST /chats/refresh` (JSON `{"imported": n}`, session+CSRF)
- `POST /billing/webhook` (`@RestController`, Stripe signature)
- `GET /chats/events` (SSE)

Controllers delegate to services (`ChatService`, `ConversationListService`, `ReplyService`, `OutboundMessageService`, `MailAccountService`, …). Some orchestration still lives in `ConversationController` (account selection, send flow) — extract before a REST layer reuses it. See `docs/API.md` for the (not yet existing) mobile contract.

## Operational notes

Schedulers: mailbox polling, IMAP IDLE (paid tiers, flag `mailbox.idle.enabled`), digests, trial-end, win-back, re-engagement, activation. Docker/CI were deliberately removed per `CLAUDE.md` (Dockerfile/compose files remain in the tree — see `INTERNAL_TODO.md`).
