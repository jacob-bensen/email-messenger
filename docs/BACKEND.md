# BACKEND.md — architecture

Repo classification: **BACKEND** (pom.xml, Spring Boot, Flyway migrations V1–V35). Sibling: `conexus-mail-mobile` (FRONTEND, currently empty scaffold).

Deviations from the generic routine's assumed stack: persistence is **Spring Data JPA** (not jOOQ — no codegen step); `mvnw` wrapper exists.

## Stack
Java 21, Spring Boot 3, Thymeleaf, Spring Security (form login + Google OIDC + remember-me), JPA/Hibernate, Flyway, PostgreSQL 16 (H2 in tests), Stripe, Jakarta Mail (IMAP/SMTP), SSE.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats workspace, mailboxes, pricing/landing, legal, PWA/SEO), `ChatService`, `ConversationListService`, `ChatEventStream` (SSE) |
| `auth` | SecurityConfig, registration/login/reset/verify flows, account settings, Google OIDC, login throttling |
| `billing` | Stripe checkout/portal/webhook, `PlanLimits` (Free 3 mailboxes/500 threads; Pro 5/unlimited; Business unlimited) |
| `email` | IMAP import/polling/IDLE, Gmail OAuth, MIME parsing, credential encryption |
| `service` | IM transform, conversation keys, reply/send (`ReplyService`), body cleaning, view records |
| `domain` / `repository` | JPA entities / Spring Data repositories |
| `team`, `digest`, `activation`, `reengagement`, `admin` | teams+invites, digests, lifecycle emails, admin metrics |

## Data model (see `src/main/resources/db/migration`)
User, Subscription (Plan FREE/PRO/BUSINESS), MailAccount (IMAP password or Gmail OAuth; encrypted creds), EmailThread (owner-scoped, `conversationKey` = participant-set key), Message (inbound/outbound, plain+html), Participant, MessageRecipient, Attachment (metadata only; `blob_ref` always null), SavedSearch, Team/TeamMember/TeamInvite, tokens (password reset, email verification), AuthEvent, PlanChangeEvent.

## Auth model
Session cookie (JSESSIONID) + optional remember-me (30 days); CSRF **on** except `/billing/webhook` and `/h2-console/**`. Everything not in the permit list in `SecurityConfig` requires authentication. **There is no token/bearer auth — a mobile client cannot authenticate today** (see INTERNAL_TODO).

## Web vs REST
Only the Thymeleaf website exists. Two JSON/non-HTML endpoints serve the browser: `POST /chats/refresh` (JSON) and `GET /chats/events` (SSE). No `/api/**` namespace exists yet. See `API.md`, `WEB.md`.
