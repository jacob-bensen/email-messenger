# Backend architecture

Spring Boot 3 / Java 21 application (`com.emailmessenger`) that imports email
over IMAP and presents it as texting-style conversations. It serves two
front-ends from one service layer:

- the **website** — server-rendered Thymeleaf pages (see `docs/WEB.md`), and
- the **REST API** under `/api/v1` for the mobile client (see `docs/API.md`,
  the canonical contract).

Persistence is **Spring Data JPA** (Hibernate) on PostgreSQL 16, schema managed
by Flyway. There is no jOOQ and no code generation step. Tests run on H2.

## Packages

| Package | Responsibility |
|---------|----------------|
| `domain` | JPA entities and enums (`User`, `MailAccount`, `EmailThread`, `Message`, `Participant`, `Attachment`, `Subscription`, `ApiToken`, …). |
| `repository` | Spring Data repositories. `EmailThreadRepository` holds the conversation-list projections and filters. |
| `email` | Mailbox integration: IMAP polling (`MailboxPollingService`, `MailboxPollingScheduler`), IMAP IDLE for paid plans (`ImapIdleService`), Gmail OAuth (`GmailOAuthClient`), MIME parsing and import (`MimeMessageParser`, `EmailImportService`), credential encryption (`CredentialEncryptor`), `MailAccountService`. |
| `service` | The IM transform: `ConversationService` (messages → bubble runs, members, signatures), `ConversationKeyService` (participant set → `conversationKey`), `EmailBodyCleaner` / `IMTransformService` (strip quotes + signatures, sanitize, markdown), `ReplyService` (SMTP send). View-model records live here (`ChatConversation`, `ConversationListItem`, `BubbleMessage`, …). |
| `web` | Thymeleaf controllers plus the services both front-ends call: `ConversationListService` (chats list, filters, counts), `ChatService` (one conversation timeline, mark-read), `MailboxScope` (resolves the `mailbox` parameter), `OutboundMessageService`, saved searches, onboarding, PWA shell, SEO, SSE (`ChatEventStream`). |
| `api` | The REST API: `ApiAuthController`, `ApiConversationController`, DTO records (`ConversationDtos`, `UserDto`, …), `ApiExceptionHandler` (JSON error envelope), `ApiAuthService` (password login → token). |
| `auth` | Registration, form login, Google OIDC, password reset, email verification, login throttle + audit (`auth_events`), account settings, and both security filter chains (`SecurityConfig`, `ApiSecurityConfig`), `ApiTokenService`. |
| `billing` | Stripe checkout/portal/webhooks, plans and limits (`PlanLimitService`, `PlanLimits`), trial nudges. |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Team invites, weekly digest email, lifecycle email drips, operator revenue dashboard. |

## How the website and the API relate

Both are thin adapters over the same services. For the conversations feature:

```
GET /chats            (ConversationController → chat.html)        ┐
GET /api/v1/conversations (ApiConversationController → JSON)      ┴→ ConversationListService.list(owner, scope, q, filter, page)

GET /chats/{key}[/pane]                                           ┐
GET /api/v1/conversations/{key}                                   ┴→ ChatService.buildFor(owner, scope, key) → ConversationService.buildChatConversation

open on web (implicit) / POST /api/v1/conversations/{key}/read    → ChatService.markRead(owner, scope, key)
```

`scope` comes from `MailboxScope.address(accounts, mailboxParam)` in both. The
API's DTOs are flat projections of the same view-model records the templates
render; the API adds no business rules of its own. **Rule: a controller (web or
API) may map and validate input, but any decision about data belongs in a
service that both can call.** Known violations are listed in
`INTERNAL_TODO.md` (notably the send flow, which still lives in
`ConversationController`).

## Auth model

Two `SecurityFilterChain`s:

1. **API chain** (`ApiSecurityConfig`, `@Order(1)`, matches `/api/**`):
   stateless, CSRF off, no request cache, no form login. `ApiTokenAuthenticationFilter`
   resolves `Authorization: Bearer cxm_…` through `ApiTokenService` and sets a
   principal whose name is the user's email. Unauthenticated → JSON `401`.
   Only `POST /api/v1/auth/login` is public.
2. **Browser chain** (`SecurityConfig`, everything else): session cookie,
   CSRF on, form login at `/login`, remember-me (`persistent_logins`), optional
   Google OIDC (only wired when client credentials are configured).

Both chains produce the same principal shape, so services resolve the user with
`UserService.requireByEmail(principal.getName())` regardless of front-end.

API tokens (`api_tokens`, V36): 32 random bytes, `cxm_` prefix, only the
SHA-256 stored; 90-day expiry; revoked individually by logout and en masse by
`AccountService.changePassword` and `PasswordResetService.consumeToken`. Rows
cascade-delete with the user.

API password login (`ApiAuthService`) uses the global `AuthenticationManager`
— the same `DbUserDetailsService` + BCrypt as form login — so the existing
`LoginAuditListener` / `AuthFailureListener` record `LOGIN_SUCCESS` /
`LOGIN_FAILURE` and `LoginThrottleService` (5 failures / 15 min per email or
IP) applies to both.

Every query for user data is scoped by the owning `User`; another user's
resource is reported as not found.

## Database schema (Flyway `V1`…`V36`)

| Area | Tables |
|------|--------|
| Mail | `email_threads` (owner, subject, `conversation_key`, unread), `messages` (sender, bodies, `sent_at`, outbound flag, Message-ID / In-Reply-To), `message_recipients`, `participants` (deduped by email), `attachments` (metadata; `blob_ref` is always null today) |
| Accounts | `users`, `mail_accounts` (IMAP/Gmail credentials encrypted, polling cursors, signature), `persistent_logins`, `password_reset_tokens`, `email_verifications`, `auth_events`, `api_tokens` |
| Billing | `subscriptions`, `plan_change_events` |
| Other | `saved_searches`, `digest_email_preferences`, `teams`, `team_members`, `team_invites` |

(`thread_notes` was created in V25 and dropped in V33.) Timestamps are
`TIMESTAMP` columns holding UTC wall-clock values (`Clock.systemUTC()`); the
API converts them to ISO instants.

Migrations are immutable once merged — add `V{n+1}__*.sql`.

## Plans

`Plan` is `FREE`, `PRO`, `BUSINESS`. `PlanLimitService.currentPlan(user)`
returns the entitled plan (active or trialing subscription, else `FREE`);
`PlanLimits` caps mailboxes (3 / 5 / unlimited) and thread history
(500 / unlimited / unlimited). Paid plans get IMAP IDLE; free accounts poll.

## Mail flow

`MailboxPollingScheduler` (flagged by `mailbox.polling.enabled`) and on-open
refreshes call `MailboxPollingService`, which fetches new UIDs (inbox + sent),
`EmailImportService` parses and threads them (`Message-ID` / `In-Reply-To` /
`References`), computes each thread's `conversationKey`, and publishes
`NewMailEvent`. `ChatEventStream` fans those out over SSE at `/chats/events`
for the website's live refresh. The API has no push channel yet.

## Testing

`./mvnw test` — JUnit 5, `@SpringBootTest` + MockMvc against H2 with real
Flyway migrations and the real security chains; GreenMail for SMTP/IMAP; one
Testcontainers Postgres test that is skipped without Docker.
