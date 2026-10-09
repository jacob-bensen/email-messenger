# Backend architecture (ConexusMail)

Reverse-engineered from the code on 2026-10-09. Unknowns are marked TBD.

## Stack (actual)
Java 21, Spring Boot 3.5.x, Spring Data **JPA** (no jOOQ in this repo), Flyway
(V1..V35 in `src/main/resources/db/migration`), PostgreSQL (prod/local), H2 (tests),
Thymeleaf, Spring Security (form login, remember-me, Google OIDC), Stripe, Jakarta Mail.

## Package layout (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, PWA, SEO, legal), SSE `ChatEventStream` |
| `service` | Conversation building, IM transform, `ReplyService`, `ConversationKeyService` |
| `email` | IMAP/Gmail import, polling, IDLE, MIME parsing, credential encryption |
| `auth` | Registration, login, password reset, email verification, account pages, security config |
| `billing` | Stripe checkout/portal/webhook, plan limits (Free/Pro/Business) |
| `team`, `digest`, `reengagement`, `activation`, `admin` | Teams/invites, digests, schedulers, admin metrics |
| `domain`, `repository` | JPA entities, Spring Data repositories |

## Schema overview
Tables: users, subscriptions, plan_change_events, mail_accounts, email_threads,
messages, message_recipients, participants, attachments, saved_searches, teams,
team_members, team_invites, digest_email_preferences, email_verifications,
password_reset_tokens, auth_events (+ persistent_logins for remember-me).
Conversations are keyed by participant-set `conversationKey`. Attachment `blob_ref`
is currently always null (bytes are not stored).

## Auth model
Session cookie (form login or Google OIDC), CSRF enabled (exempt: `/h2-console/**`,
`/billing/webhook`), remember-me 30 days. Everything not on the permit-list in
`SecurityConfig` requires authentication. **There is no token/API auth for a mobile client yet.**

## Web vs REST
The app is server-rendered only. The only `@RestController` is the Stripe webhook.
There is no REST API for the mobile client (see `API.md`). Business logic lives in
services; controllers are thin-ish (some orchestration remains in `ConversationController`
— see `INTERNAL_TODO.md`).
