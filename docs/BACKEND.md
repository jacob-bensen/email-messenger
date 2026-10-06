# BACKEND — email-messenger ("ConexusMail")

Spring Boot 3 / Java 21 monolith. Server-rendered Thymeleaf website + persistence + all business rules.
**Deviation from the generic target stack:** persistence is Spring Data **JPA/Hibernate** (`ddl-auto: validate`), not jOOQ; there is no codegen step.

## Package layout (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`ConversationController` = /chats workspace), SSE (`ChatEventStream`), PWA, SEO, mailbox + saved-search controllers |
| `service` | IM transform: `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`, view records (`ConversationListItem`, `BubbleMessage`, `ChatConversation`) |
| `email` | IMAP/Gmail import, polling (`MailboxPollingService`), IDLE, credential encryption |
| `auth` | Form login, Google OIDC, registration, password reset, email verification, account settings, throttling |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digests, lifecycle emails, operator metrics |
| `domain` / `repository` | JPA entities / Spring Data repos |

## Schema
Flyway migrations `V1`–`V35` in `src/main/resources/db/migration` (immutable once merged). Core tables: users, subscriptions, mail_accounts, email_thread, message (+ recipients, attachments, participants), saved_searches, teams/invites, auth_events, plan_change_events, persistent remember-me tokens. Conversations are keyed by `email_thread.conversation_key` (participant set, V32).

## Auth model
Session cookie (form login, `email`/`password`) + remember-me (30 days, persistent token table) + optional Google OIDC. CSRF **on** (exempt: `/h2-console/**`, `/billing/webhook`). Everything not on the permit list in `auth/SecurityConfig` requires authentication. **There is no token/bearer auth** — a mobile client cannot authenticate yet (see INTERNAL_TODO).

## Web vs REST
Only HTML endpoints exist, plus `POST /chats/refresh` (`@ResponseBody`), SSE `/chats/events`, and the Stripe webhook (`@RestController`). No `/api/**` namespace. See `docs/API.md`, `docs/WEB.md`.

## Docs
`docs/API.md` canonical contract · `docs/WEB.md` pages · `CLAUDE.md` conventions · `master/` holds historical PLAN/BACKLOG/CHANGELOG (legacy location).
