# BACKEND.md — architecture

Repo classification: BACKEND (pom.xml, Spring Boot, Flyway migrations). Sibling: `conexus-mail-mobile` (FRONTEND, Flutter; currently only a README).

## Stack (actual)
Java 21, Spring Boot 3, Spring Data **JPA/Hibernate** (NOT jOOQ — no codegen step), PostgreSQL 16 (H2 in tests), Flyway (`src/main/resources/db/migration`, V1..V35), Thymeleaf, Maven wrapper.

## Modules (`com.emailmessenger.*`)
| Package | Role |
|---|---|
| `auth` | Form login + Google OIDC, registration, password reset, email verification, account settings, login throttle, auth audit events |
| `billing` | Stripe checkout/portal/webhook, plans (Free/Pro/Business), plan limits (`PlanLimitService`/`PlanLimitKind`), trial nudges |
| `email` | IMAP/Gmail-OAuth mailbox access, polling, IMAP IDLE, MIME parsing/import (`EmailImportService`), credential encryption |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), conversation view models, `ConversationKeyService`, `ReplyService` |
| `web` | Thymeleaf controllers (chats, mailboxes, pricing, legal, PWA, SEO, saved searches), SSE `ChatEventStream`, `OutboundMessageService` |
| `team` | Teams + invites |
| `digest`, `reengagement`, `activation` | Scheduled emails |
| `admin` | `/admin/revenue` metrics + operator actions |
| `domain`, `repository` | JPA entities / Spring Data repos |

## Data model (see migrations)
`users`, `persistent_logins`, `subscriptions`, `plan_change_events`, `mail_accounts` (auth_type IMAP/Gmail OAuth, last_seen_uid, last_seen_sent_uid, signature, polling policy), `email_threads` (owner, `conversation_key` = participant-set key, unread), `messages` (+ outbound flag, V30), `message_recipients`, `participants`, `attachments` (`blob_ref` always null — bytes not stored), `saved_searches`, `digest_email_preferences`, `password_reset_tokens`, `email_verifications`, `auth_events`, `teams`, `team_members`, `team_invites`.

## Auth model
Session cookie (JSESSIONID) from form login (`email`/`password` params) or Google OIDC; remember-me (30 d, JDBC token repo). CSRF ON for everything except `/h2-console/**` and `/billing/webhook`. Everything not whitelisted in `SecurityConfig` requires authentication. **No token/API-key auth exists** — a mobile client cannot authenticate non-interactively yet.

## Web vs REST
All current endpoints are Thymeleaf/redirect-style controllers. JSON exists only at `POST /chats/refresh` (`{"imported":n}`) and the SSE stream `GET /chats/events`. There is **no REST API**; see `docs/API.md`. Services are already separated from controllers (controllers call `ConversationListService`, `ChatService`, `ReplyService`, `OutboundMessageService`, `MailAccountService`, ...), so a REST layer should reuse them.

## Legacy docs
`master/` (tracked directory) holds an earlier workflow's BACKLOG/CHANGELOG/PLAN/MASTER_ACTIONS. Left untouched; root `CHANGELOG.md` is the one for this routine.
