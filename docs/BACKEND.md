# BACKEND.md — architecture (reverse-engineered 2026-11-10)

Spring Boot 3 / Java 21 / Maven. **Actual stack differs from the sync-routine's assumed stack:** persistence is
Spring Data **JPA** (not jOOQ), schema by **Flyway** (`src/main/resources/db/migration`, V1..V35), PostgreSQL in
prod, H2 in tests. Views are Thymeleaf. There is **no REST/JSON API** yet (see `API.md`).

## Packages (`com.emailmessenger`)
| package | role |
|---|---|
| `auth` | form login + Google OIDC, registration, email verification, password reset, account settings, login throttling, auth-event audit |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), trial nudges |
| `email` | IMAP import/polling/IDLE, Gmail OAuth, MIME parsing, credential encryption |
| `service` | conversation building: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ReplyService`, `ConversationKeyService` (participant-set key) |
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, pricing, legal, PWA, SSE `/chats/events`) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | teams/invites, digests, lifecycle emails, operator metrics |
| `domain` / `repository` | JPA entities / Spring Data repos |

## Data model (overview)
User, Subscription (Free/Pro/Business), MailAccount (IMAP password or Gmail OAuth, encrypted creds), EmailThread
(owner + `conversationKey`), Message (+ outbound flag), MessageRecipient, Participant, Attachment (`blob_ref` always
null today), SavedSearch, Team/TeamMember/TeamInvite, AuthEvent, PlanChangeEvent, token tables.

## Auth model
Server session (cookie) via Spring Security form login; optional Google OIDC. CSRF enabled (only `/billing/webhook`
and `/h2-console/**` exempt). Everything except public marketing/legal/PWA/auth routes requires authentication.
**No token auth for a mobile client exists** (TBD — see INTERNAL_TODO).

## Web vs API
Controllers call services directly (`MailAccountService`, `ConversationService`, `ReplyService`, `OutboundMessageService`,
`ChatService`). Any future `/api/**` controllers must reuse these services.

Pre-existing project docs live in `CLAUDE.md` (roadmap) and `master/` (PLAN/BACKLOG/CHANGELOG of an earlier routine).
