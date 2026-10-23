# BACKEND

Repo classification: BACKEND (pom.xml, Spring Boot 3, Flyway V1..V35, Thymeleaf). Sibling: `conexus-mail-mobile` (FRONTEND, Flutter, currently only a README).

## Deviations from the generic target stack
- Persistence is **Spring Data JPA** (entities in `domain/`, repos in `repository/`), NOT jOOQ. No codegen step.
- Build is Maven wrapper (`./mvnw`), Java 21, PostgreSQL 16 prod / H2 tests.
- There is **no REST API for a mobile client yet** (see API.md). All endpoints are server-rendered, session-cookie + CSRF.

## Modules (`com.emailmessenger.*`)
| Package | Role |
|---|---|
| `auth` | Form login (`/login`, email+password), Google OIDC, registration, email verification, password reset, login throttle, remember-me (30d, persistent token repo), account/appearance settings |
| `billing` | Stripe checkout/portal/webhook (`/billing/webhook`, CSRF-exempt), plans Free/Pro/Business, `PlanLimitService` |
| `email` | IMAP (`JakartaImapClient`), IDLE (`ImapIdleService`), Gmail OAuth, polling (`MailboxPollingService`), MIME parsing, import, `CredentialEncryptor` |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), conversation view models (`ChatConversation`, `BubbleRun`...), `ReplyService`, `ConversationKeyService` |
| `web` | Controllers (chats, mailboxes, saved searches, PWA, SEO, legal), `ChatService`, `OutboundMessageService`, `ChatEventStream` (SSE) |
| `team`, `digest`, `admin`, `activation`, `reengagement` | Teams/invites, weekly digests, operator revenue metrics, lifecycle emails |
| `domain` / `repository` | JPA entities / Spring Data repos |

## Schema overview
Flyway `src/main/resources/db/migration/V1..V35` (immutable once merged). Core: users, subscriptions, mail_account, email_thread, message, message_recipient, participant, attachment (`blob_ref` always null today), saved_search, team/team_member/team_invite, auth_event, digest prefs. Conversations are keyed by participant-set `conversationKey`.

## Auth model
Session cookie (JSESSIONID) + remember-me; CSRF on for everything except `/h2-console/**` and `/billing/webhook`. Everything not on the `permitAll` list in `SecurityConfig` requires authentication. Admin pages use `AdminAuthorizer`. No token/bearer auth exists — a mobile client would need one (TBD, see INTERNAL_TODO).

## Web vs API
Only web exists. Controllers call services (`ChatService`, `ConversationListService`, `OutboundMessageService`, `MailAccountService`); a future REST layer must reuse these, not fork logic.

## Extra docs
`master/` (PLAN.md, BACKLOG.md, CHANGELOG.md, MASTER_ACTIONS.md) is the pre-existing product-ops planning folder; root `CHANGELOG.md` is the sync-routine changelog.
