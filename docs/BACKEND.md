# Backend architecture

Reverse-engineered 2026-10-27 from the code. Unknowns are marked TBD.

## Stack
Java 21, Spring Boot 3, Spring Data **JPA** (not jOOQ — no jOOQ dependency in `pom.xml`),
Thymeleaf, Flyway (`V1`..`V35`, `src/main/resources/db/migration`), PostgreSQL 16 (H2 in tests), Maven wrapper.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers: chats workspace, mailboxes, Gmail OAuth connect, saved searches, pricing/landing/legal/SEO/PWA, SSE stream (`ChatEventStream`) |
| `service` | Conversation building (`ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`), `ReplyService` |
| `email` | IMAP import/polling/IDLE, MIME parsing, Gmail OAuth client, credential encryption |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `auth` | Form login, Google OIDC, registration, password reset, email verification, account settings, login throttle |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Team invites, weekly digests, lifecycle emails, operator metrics (`/admin/revenue`) |

## Auth model
Session cookie (`JSESSIONID`) via form login (`/login`, params `email`/`password`) or Google OIDC;
remember-me (30 days). CSRF is **enabled** (exempt: `/h2-console/**`, `/billing/webhook`).
`anyRequest().authenticated()` except the public allow-list in `auth/SecurityConfig.java`.
There is **no token (bearer/JWT) authentication** — TBD for mobile.

## Domain
`User`, `Subscription`/`Plan`, `MailAccount` (IMAP or Gmail OAuth), `EmailThread`, `Message`,
`MessageRecipient`, `Participant`, `Attachment` (`blob_ref` always null), `SavedSearch`, `Team*`.
Chats are keyed by participant-set `conversationKey` (`/chats/{key}`).

## Web vs REST
The app is server-rendered. The only JSON endpoints are `POST /chats/refresh` and
`POST /billing/webhook` (Stripe). **No mobile REST API exists** — see `API.md`.
