# BACKEND.md — architecture

Classification: this repo is the BACKEND (Spring Boot 3, Flyway, `src/main/resources/templates`).
Deviations from the generic target stack: persistence is **Spring Data JPA/Hibernate** (no jOOQ,
no codegen); CI and Docker-based deploys were deliberately removed (see CLAUDE.md).

## Modules (`com.emailmessenger.*`)
| Package | Role |
|---|---|
| `auth` | Form login, Google OIDC, registration, email verification, password reset, account settings, login throttling/audit, `SecurityConfig` |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), trial nudges |
| `email` | Mailbox connect, IMAP import/polling/IDLE, Gmail OAuth, MIME parsing, credential encryption |
| `service` | Conversation building: threading, quoted-reply stripping (`EmailBodyCleaner`), IM bubble transform, `ReplyService`, conversation keys |
| `web` | Thymeleaf controllers for /chats, mailboxes, pricing/landing, PWA, SEO, SSE stream |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, weekly digests, lifecycle emails, admin metrics |
| `domain`, `repository` | JPA entities and Spring Data repositories |

## Data model (Flyway `V1`..`V35`, immutable once merged)
Tables: users, subscriptions, plan_change_events, mail_accounts, participants, email_threads,
messages, message_recipients, attachments (`blob_ref` always null today), saved_searches, teams,
team_members, team_invites, thread_notes, digest_email_preferences, email_verifications,
password_reset_tokens, auth_events, persistent_logins.
Conversations in the chats UI are keyed by participant-set `conversationKey`, not thread id.

## Auth model
Session cookie (JSESSIONID) + remember-me (30d) + CSRF on all POSTs (exempt: `/billing/webhook`,
`/h2-console/**`). Everything not in the `SecurityConfig` permit-list requires authentication.
There is **no token/bearer auth** — see INTERNAL_TODO.md.

## Web vs REST
All HTTP surface is server-rendered Thymeleaf except `POST /billing/webhook` (`@RestController`)
and the SSE stream `GET /chats/events`. There is no JSON API for the mobile client yet
(docs/API.md). When one is added it must call the same services as the web controllers.
