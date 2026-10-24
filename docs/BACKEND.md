# BACKEND — architecture (reverse-engineered 2026-10-24)

Spring Boot 3 / Java 21 monolith ("ConexusMail"). Server-rendered Thymeleaf
website + persistence + all business logic. **There is no REST/JSON API yet**
(see `API.md`).

## Deviations from the generic target stack
- Persistence is **Spring Data JPA/Hibernate**, not jOOQ. No codegen step.
- Flyway migrations: `src/main/resources/db/migration/V1..V35`. Immutable once merged.
- Auth is **session cookie + CSRF** (form login, Google OIDC, remember-me), not tokens.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`ConversationController` = /chats workspace, `MailboxController`, `GmailConnectController`, `SavedSearchController`, `PricingController`, `SeoController`, `PwaController`, `ChatEventsController` SSE), view services (`ChatService`, `ConversationListService`, `OutboundMessageService`) |
| `service` | Mail → IM transform: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`; view-model records (`ChatConversation`, `BubbleRun`, ...) |
| `email` | IMAP/SMTP: import, polling, IDLE, Gmail OAuth, credential encryption |
| `auth` | Spring Security config, registration, login throttle, password reset, email verification, Google OIDC, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), trial nudges |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, weekly digests, lifecycle emails, admin metrics |
| `domain`, `repository` | JPA entities and Spring Data repos |

## Data model (tables)
users, persistent_logins, auth_events, email_verifications, password_reset_tokens,
mail_accounts, participants, email_threads (carries `conversationKey`), messages,
message_recipients, attachments (`blob_ref` always null — bytes not stored),
subscriptions, plan_change_events, teams, team_members, team_invites,
saved_searches, digest_email_preferences, thread_notes.

A *conversation* = all messages sharing a participant-set `conversationKey`,
merged across threads, shown as chat bubbles.

## Auth model
`SecurityConfig`: public = marketing/legal/login/register/password/verify/webhook/static;
everything else `authenticated()`. CSRF on except `/billing/webhook`, `/h2-console/**`.
Plan tiers Free/Pro/Business enforced by `PlanLimitService`.

## Web vs. API
Controllers call services directly (`UserService`, `ChatService`, `ReplyService`,
`MailAccountService`...). A future JSON API MUST reuse these services; the
`web` services are currently package-private and would need widening/moving.
