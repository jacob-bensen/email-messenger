# BACKEND.md — architecture (reverse-engineered 2026-10-06)

Spring Boot 3.5 / Java 21 / Maven monolith ("ConexusMail"). **Deviation from the
generic target stack:** persistence is **Spring Data JPA (Hibernate)**, not jOOQ;
there is no jOOQ codegen. Flyway migrations (`src/main/resources/db/migration`,
V1–V35) own the schema; Postgres in prod, H2 in tests.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `auth` | Spring Security config (form login + Google OIDC, remember-me via `persistent_logins`, CSRF on), registration, password reset, email verification, login throttling, account settings |
| `billing` | Stripe checkout/portal/webhook, plans (Free/Pro/Business), `PlanLimitService`, trial nudges |
| `email` | Mailbox integration: IMAP client, IMAP IDLE (paid tiers), Gmail OAuth, polling scheduler, MIME parsing, import into threads, credential encryption |
| `service` | Conversation logic: `ConversationService`, `ConversationKeyService` (participant-set key), `IMTransformService`/`EmailBodyCleaner` (quote stripping, bubbles), `ReplyService`; view records (`ChatConversation`, `ConversationListItem`, `BubbleMessage`, `BubbleRun`) |
| `web` | Thymeleaf controllers, `ChatService`, `ConversationListService`, `OutboundMessageService`, `ChatEventStream` (SSE), SEO/PWA/legal, model advices |
| `domain` / `repository` | JPA entities / Spring Data repositories |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams + invites, digests, onboarding/win-back emails, admin revenue/metrics |

## Schema overview (tables)
users, subscriptions, plan_change_events, mail_accounts, participants,
email_threads (has `conversation_key`, owner, unread), messages (+ outbound flag),
message_recipients, attachments (`blob_ref` always null today), saved_searches,
digest_email_preferences, teams/team_members/team_invites, password_reset_tokens,
email_verifications, auth_events, persistent_logins. (`thread_notes` was dropped in V33.)

## Auth model
Session cookie (`JSESSIONID`) + optional 30-day remember-me. CSRF is **enabled**
for everything except `/billing/webhook` and `/h2-console/**`. Public paths are
listed in `auth/SecurityConfig.java`; everything else requires authentication.
There is **no token/bearer auth** — nothing a mobile client can use without
emulating a browser form login. See `docs/API.md`.

## Web ↔ API relationship
Today there is no REST API; all interactive behaviour is server-rendered
Thymeleaf returning HTML/redirects, plus one SSE endpoint. Business logic lives
in services (`ChatService`, `ReplyService`, `OutboundMessageService`,
`MailAccountService`, …) which controllers call; a future JSON API must call the
same services (see `docs/API.md` §Proposed).
