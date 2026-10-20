# Backend architecture

Classification: this repo is the **BACKEND** (pom.xml, Spring Boot, Flyway migrations V1–V35, Thymeleaf).
Deviation from the generic target stack: persistence is **Spring Data JPA/Hibernate (ddl-auto=validate)**, not jOOQ.
There is no jOOQ codegen step. Schema changes = new Flyway migration + entity update.

## Stack
Java 21, Spring Boot 3, Thymeleaf, JPA, Flyway, PostgreSQL 16 (prod/local), H2 (tests), Spring Security
(form login + remember-me + optional Google OIDC), Stripe (billing), Jakarta Mail (IMAP/SMTP), SSE.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats, mailboxes, saved searches, pricing, legal, PWA, SEO), `ChatEventStream` (SSE) |
| `service` | IM transform, `ConversationService`, `ReplyService` (SMTP send), `ConversationKeyService`, view records (`ChatConversation`, `BubbleRun`, ...) |
| `email` | IMAP import/polling/IDLE, MIME parsing, mailbox credentials (encrypted), Gmail OAuth |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `auth` | Security config, registration, password reset, email verification, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | teams/invites, weekly digest, lifecycle emails, operator metrics |

## Data model (overview)
users, subscriptions (FREE/PRO/BUSINESS), mail_accounts (IMAP password or Gmail OAuth, polling policy,
last-seen UIDs, signature), email_thread (owner, unread, `conversation_key`), message (outbound flag),
participant, message_recipient, attachment (`blob_ref` always null — bytes not stored), saved_searches,
digest_email_preferences, password_reset_tokens, email_verifications, auth_events, plan_change_events,
teams/team_members/team_invites, persistent_logins (remember-me).

Core concept: **conversation key** = stable key of the participant set (excluding the owner's addresses).
A "chat" merges all email threads with that participant set into one timeline.

## Auth model
Session cookie (JSESSIONID) + CSRF token on every form POST (exempt: `/billing/webhook`, `/h2-console/**`);
remember-me (30 days, persistent tokens). Everything except the public list in `SecurityConfig` requires auth.
**There is no token/bearer auth** — a mobile client cannot authenticate today (see INTERNAL_TODO).

## Web vs REST
All user-facing behavior is server-rendered HTML. Only JSON endpoint: `POST /chats/refresh`.
Controllers (notably `ConversationController`) still hold some logic (recipient parsing, signature
appending, mailbox scoping) that a JSON API must reuse — extract to services first (INTERNAL_TODO).
