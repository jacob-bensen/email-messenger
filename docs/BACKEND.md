# BACKEND.md — architecture

Repo classification: **BACKEND** (pom.xml + Spring Boot + Flyway under `src/main/resources/db/migration`).
Sibling: `conexus-mail-mobile` (FRONTEND, Flutter; currently an empty scaffold).

## Stack (as implemented)
Java 21, Spring Boot 3, Spring Data **JPA** (not jOOQ — the routine's default assumption does not apply),
Thymeleaf, Spring Security (form login + remember-me + optional Google OIDC), Flyway, PostgreSQL 16
(H2 in tests), Jakarta Mail (IMAP/SMTP), Stripe, jsoup. Maven wrapper (`./mvnw`).

## Packages (`com.emailmessenger.*`)
| Package | Role |
|---|---|
| `domain` | JPA entities (User, MailAccount, EmailThread, Message, Participant, Attachment, Subscription, Team*, SavedSearch, …) |
| `repository` | Spring Data repositories |
| `email` | IMAP import/polling/IDLE, Gmail OAuth, MIME parsing, credential encryption |
| `service` | Conversation building, IM transform (`IMTransformService`, `EmailBodyCleaner`), `ReplyService`, `ConversationKeyService` |
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, pricing, legal, PWA) + SSE `/chats/events` |
| `auth` | Security config, registration, login throttle, password reset, email verification, account pages |
| `billing` | Stripe checkout/portal/webhook, plan limits (Free/Pro/Business) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digests, lifecycle schedulers, admin metrics |

## DB schema overview
Tables (Flyway V1…V35): users, subscriptions, plan_change_events, mail_accounts, email_threads, messages,
message_recipients, participants, attachments (`blob_ref` always null today), saved_searches, teams,
team_members, team_invites, thread_notes, password_reset_tokens, email_verifications, auth_events,
digest_email_preferences, persistent_logins. Conversations are keyed by participant-set `conversationKey`.

## Auth model
Session cookie (`JSESSIONID`) + CSRF + optional `remember-me` (30 days, JDBC token store). Everything except the
public allow-list in `SecurityConfig` requires authentication. **There is no token/stateless auth.** The only CSRF
exemptions are `/h2-console/**` and `/billing/webhook`.

## Web vs REST
There is currently **no REST API**. All state-changing behavior is reached via server-rendered form POSTs that call
service-layer classes (`ChatService`, `ReplyService`, `OutboundMessageService`, `MailAccountService`, …). A future
mobile API must be thin controllers over those same services (see `API.md`).
