# Backend architecture

Classification: this repo is the BACKEND (Maven + Spring Boot + Flyway + Thymeleaf).
Sibling: `conexus-mail-mobile` (Flutter client, currently an empty scaffold).

> Divergence from the generic routine template: persistence is **Spring Data JPA
> (Hibernate)**, not jOOQ. There is no jOOQ codegen. Flyway owns the schema; JPA
> entities in `com.emailmessenger.domain` map onto it.

## Stack
Java 21, Spring Boot 3, Spring Security (form login + Google OIDC + remember-me),
Spring Data JPA, Flyway (V1..V35), Thymeleaf, Jakarta Mail (IMAP/SMTP), jsoup,
Stripe SDK. PostgreSQL 16 in prod/dev, H2 in tests, GreenMail for mail ITs.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers: chats workspace, mailboxes, saved searches, pricing/landing, legal, SEO, PWA, SSE (`/chats/events`) |
| `auth` | Registration, login, OAuth, password reset, email verification, account settings, `SecurityConfig` |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`) |
| `email` | IMAP import/poll/IDLE, Gmail OAuth, MIME parsing, mailbox accounts, credential encryption |
| `service` | Conversation/IM transform, `ChatConversation`, `ReplyService`, body cleaning, conversation keys |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams + invites, digests, lifecycle emails, admin metrics |

## Schema overview (tables from Flyway)
`users`, `subscriptions`, `plan_change_events`, `mail_accounts` (IMAP/Gmail creds
encrypted, last-seen UIDs, polling policy, signature), `participants`,
`email_threads` (owner, unread, `conversation_key`), `messages` (incl. outbound
flag), `message_recipients`, `attachments` (`blob_ref` always null today),
`saved_searches`, `teams`/`team_members`/`team_invites`, `auth_events`,
`email_verifications`, `password_reset_tokens`, `digest_email_preferences`,
`persistent_logins`. (`thread_notes` dropped in V33.) Migrations are immutable.

## Auth model
Session-cookie auth with CSRF protection (only `/billing/webhook` and
`/h2-console/**` are exempt). Everything except the public allow-list in
`SecurityConfig` requires an authenticated session. **There is no token/bearer
auth and no `/api/**` surface** — a mobile client cannot authenticate today.

## Web vs API
The Thymeleaf controllers are the only consumers of the service layer; no REST
API exists. Controllers resolve the user via `UserService.requireByEmail(principal)`
then call `ChatService`, `ConversationListService`, `MailAccountService`, etc.
When a JSON API is added it must call those same services (see INTERNAL_TODO).
Existing JSON/SSE endpoints: `POST /chats/refresh`, `GET /chats/events`,
`POST /billing/webhook`.
