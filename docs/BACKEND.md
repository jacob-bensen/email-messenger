# BACKEND architecture

Repo role: **BACKEND** (evidence: `pom.xml`, Spring Boot 3 / Java 21, Flyway migrations V1–V35, Thymeleaf templates).
Sibling: `conexus-mail-mobile` (FRONTEND, Flutter; currently only a README).

## Stack (actual, differs from the generic target)
- Spring Boot 3, Java 21, Maven wrapper. Persistence is **Spring Data JPA** (not jOOQ — no codegen step).
- PostgreSQL 16 (prod/local), H2 in tests. Flyway migrations in `src/main/resources/db/migration` (immutable once merged).
- Thymeleaf server-rendered UI (`src/main/resources/templates`). No REST/JSON API for a mobile client exists yet (see `API.md`).

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `auth` | Form login + Google OIDC, registration, password reset, email verification, account settings, login throttling, `SecurityConfig` |
| `billing` | Stripe checkout/portal/webhook, plans & limits (`PlanLimitService`), trial/cancel flows |
| `email` | Mailbox connect (IMAP password / Gmail OAuth), polling, IMAP IDLE, MIME parsing, import, credential encryption |
| `service` | Domain services: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ReplyService`, `ConversationKeyService`, chat view models |
| `domain` | JPA entities (User, MailAccount, EmailThread, Message, Participant, Attachment, Subscription, Team*, SavedSearch, …) |
| `repository` | Spring Data repositories |
| `web` | Thymeleaf controllers (chats, mailboxes, gmail connect, saved searches, legal, SEO, PWA) |
| `admin`, `digest`, `activation`, `reengagement`, `team` | Operator metrics, digests, lifecycle emails, team invites |

## Auth model
- Session cookie (JSESSIONID) + remember-me (30 days, persistent token repo). CSRF **enabled** except `/billing/webhook` and `/h2-console/**`.
- Public routes whitelisted in `SecurityConfig`; everything else requires authentication. Principal name = user email.
- Admin pages gated by `AdminAuthorizer`.
- **No token/bearer auth exists** — required before a mobile client can call the server (see `INTERNAL_TODO.md`).

## Core data concepts
- Conversations are keyed by participant-set `conversationKey` (V32), scoped by mailbox address; threads merged into a texting-style timeline.
- Plans Free/Pro/Business with limits enforced server-side in `PlanLimitService`.
