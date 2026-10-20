# BACKEND — architecture

Repo classification: BACKEND (Spring Boot 3, Java 21, Maven, Flyway, Thymeleaf). Sibling: `conexus-mail-mobile` (Flutter client, not yet scaffolded).

> Deviation from the routine's assumed stack: persistence is **Spring Data JPA/Hibernate**, not jOOQ. No codegen step exists.

## Modules (`com.emailmessenger.*`)
| Package | Role |
|---|---|
| `auth` | Form login + Google OIDC, remember-me (JDBC tokens), login throttling, email verification, password reset, account pages |
| `billing` | Stripe checkout/portal/webhook, plan limits (Free/Pro/Business) |
| `email` | IMAP import (poll + IDLE), Gmail OAuth, MIME parsing, credential encryption |
| `service` | IM transform (quote stripping, bubble runs), conversation keys, reply/outgoing mail |
| `web` | Thymeleaf controllers: `/chats` workspace, mailboxes, saved searches, PWA, SEO, legal |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digests, lifecycle emails, operator revenue dashboard |
| `domain` / `repository` | JPA entities / Spring Data repos |

## Schema
Flyway migrations `src/main/resources/db/migration` (V1..V35, immutable). Tables: users, participants, email_threads, messages, message_recipients, attachments (`blob_ref` always null today), mail_accounts, subscriptions, plan_change_events, saved_searches, teams, team_members, team_invites, digest_email_preferences, auth_events, email_verifications, password_reset_tokens, persistent_logins. PostgreSQL in prod, H2 in tests.

## Auth model
Server-side session cookie (`JSESSIONID`) + optional remember-me cookie, CSRF **enabled** (exempt: `/billing/webhook`, `/h2-console/**`). Everything not on the permitAll list requires login. There is **no token/API-key auth** — a mobile client cannot authenticate today (see INTERNAL_TODO).

## Web vs API
There is no REST API. All endpoints are server-rendered MVC; the only JSON/SSE endpoints are `POST /chats/refresh`, `GET /chats/events` (SSE) and the Stripe webhook. Controllers delegate to services (`ConversationService`, `ChatService`, `ReplyService`, `MailAccountService`, ...), which is the layer a future `/api/v1` must reuse.

## Commands
`./mvnw test` · `./mvnw clean package` · run: see CLAUDE.md.
