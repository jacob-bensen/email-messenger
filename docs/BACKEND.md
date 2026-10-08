# Backend architecture

Repo classification: BACKEND (pom.xml, Spring Boot, Flyway migrations V1–V35).
Sibling: `conexus-mail-mobile` (Flutter client; currently an empty scaffold).

## Stack (as implemented)
Java 21, Spring Boot 3, Spring Data **JPA** (not jOOQ — no codegen step), Flyway,
PostgreSQL (H2 in tests), Thymeleaf, Spring Security, Stripe (stripe-java),
Jakarta Mail (IMAP/SMTP), jsoup. Maven wrapper (`./mvnw`).

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, landing/pricing/legal/PWA/SEO), SSE stream `/chats/events` |
| `auth` | Security config, form login + Google OIDC, registration, password reset, email verification, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (Free/Pro/Business) |
| `email` | IMAP import, polling, IMAP IDLE, Gmail OAuth, MIME parsing, credential encryption |
| `service` | IM transform: conversation keys, quoted-reply stripping, bubble runs, replies |
| `domain` / `repository` | JPA entities / Spring Data repositories |
| `team`, `digest`, `activation`, `reengagement`, `admin` | teams + invites, digests, lifecycle emails, admin metrics |

## Schema overview (Flyway `src/main/resources/db/migration`)
Tables: users, subscriptions, plan_change_events, mail_accounts, email_threads
(has `conversation_key`, owner), messages (incl. outbound flag), message_recipients,
participants, attachments (`blob_ref` always null today), saved_searches,
digest_email_preferences, teams, team_members, team_invites, password_reset_tokens,
email_verifications, auth_events, persistent_logins (remember-me).
Migrations are immutable; add `V36__*.sql` next.

## Auth model
Session-cookie based: form login (`POST /login`, params `email`,`password`, optional
`remember-me`), Google OIDC (only when `GOOGLE_CLIENT_ID` set), CSRF enabled (exempt:
`/billing/webhook`, `/h2-console/**`). Everything not whitelisted in `SecurityConfig`
requires authentication. **There is no token/API auth yet** — see `docs/API.md`.

## Web vs REST
All user-facing functionality is server-rendered Thymeleaf. Controllers delegate to
services (`ChatService`, `ConversationListService`, `ReplyService`,
`MailAccountService`, …). No `@RestController` exists except Stripe's webhook;
a mobile REST layer must call these same services (see INTERNAL_TODO).
