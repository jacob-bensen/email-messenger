# Backend architecture

Spring Boot 3 / Java 21 monolith, PostgreSQL 16 (H2 in tests), Flyway (V1–V35),
Spring Data **JPA** (not jOOQ — the routine's default stack assumption does not
apply here), Thymeleaf server-rendered UI, Maven.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `auth` | form login + Google OIDC, registration, verification, password reset, account settings, login throttling |
| `billing` | Stripe checkout/portal/webhook, plans, plan limits |
| `email` | IMAP/Gmail-OAuth import, polling, IMAP IDLE, MIME parsing |
| `service` | IM transform, chat conversations, conversation keys, reply/outbound |
| `web` | Thymeleaf controllers, chat SSE stream, mailboxes, saved searches, PWA, SEO |
| `team`, `digest`, `activation`, `reengagement`, `admin` | teams/invites, digests, lifecycle emails, admin metrics |
| `domain` / `repository` | JPA entities / Spring Data repositories |

## Schema (Flyway `src/main/resources/db/migration`)
Tables: users, subscriptions, plan_change_events, mail_accounts, email_threads
(has `conversation_key`), messages, message_recipients, participants,
attachments (`blob_ref` always null today), saved_searches, teams, team_members,
team_invites, digest_email_preferences, password_reset_tokens,
email_verifications, auth_events, persistent_logins. Migrations are immutable.

## Auth model
Session-cookie auth (form login or Google OIDC), CSRF enabled (ignored only for
`/h2-console/**` and `/billing/webhook`). Everything except the public list in
`SecurityConfig` requires authentication. **There is no token/bearer auth, so a
mobile client cannot authenticate today.**

## Web vs API
All endpoints are `@Controller` HTML/redirect endpoints (the only non-HTML ones
are the SSE stream `/chats/events`, PWA/SEO assets, and the Stripe webhook).
Controllers delegate to services (`ChatService`, `ConversationListService`,
`OutboundMessageService`, `MailAccountService`, ...). A future REST layer must
reuse those services.

## Commands
`./mvnw clean package`, `./mvnw test`, see CLAUDE.md.
