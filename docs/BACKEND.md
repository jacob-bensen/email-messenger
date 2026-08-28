# BACKEND — email-messenger (ConexusMail)

Spring Boot 3.5 / Java 21 / Hibernate + JPA / PostgreSQL 16 / Flyway / Maven.
Serves both the public website (Thymeleaf) and the REST API for the
`conexus-mail-mobile` client. All persistence and business logic live
here — the mobile client mirrors rules for optimistic UX but the server is
always authoritative.

## Package layout (`com.emailmessenger.*`)

| Package | Purpose |
| --- | --- |
| `EmailMessengerApplication` | Boot entry; `@EnableScheduling` + `@EnableAsync`; publishes a `Clock` bean. |
| `admin` | `/admin/**` operator dashboards (revenue, funnel, digests). Gated by `AdminProperties.emails`. |
| `activation` | Onboarding drip scheduler + services. |
| `auth` | Form + Google OIDC login, password reset, email verification, `SecurityConfig`, `AccountController`. |
| `billing` | Stripe checkout, portal, webhook, `PlanPricing`, `BillingProperties`. |
| `digest` | Daily/weekly email digest generator + opt-out. |
| `domain` | JPA entities (classes, not records). |
| `email` | IMAP polling + IDLE, body cleaning, thread building, outbound SMTP. |
| `reengagement` | Long-quiet-user re-engagement drip. |
| `repository` | Spring Data `JpaRepository` interfaces. |
| `service` | Cross-cutting services (`ConversationService`, `MailPreferenceService`, …). |
| `team` | Teams, invites, membership. |
| `web` | Thymeleaf controllers (`ConversationController`, `MailboxController`, `PricingController`, …) and the SSE stream (`ChatEventsController`). |

## Persistence

- Postgres 16 in prod + local dev. H2 in tests (`src/test/resources/application.yml`).
- `spring.jpa.hibernate.ddl-auto=validate` — schema is owned by Flyway.
- Flyway migrations under `src/main/resources/db/migration/V{n}__*.sql`, immutable once merged.
- Key tables (V1 → V35): `users`, `subscriptions`, `mail_accounts`,
  `email_threads` (`owner_id`, `conversation_key`, `unread`),
  `messages`, `message_recipients`, `participants`, `attachments`,
  `saved_searches`, `teams`, `team_members`, `team_invites`,
  `plan_change_events`, `digest_email_preferences`, `user_mail_preferences`.
- Symmetric encryption of stored IMAP credentials via
  `mailbox.encryption.password` + `mailbox.encryption.salt`
  (`CredentialEncryptor`).

## Auth model

- Session-cookie (JSESSIONID) form login + remember-me
  (`JdbcTokenRepositoryImpl` persistent tokens).
- Google OIDC opt-in (`GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET`).
  Wired only when a `ClientRegistrationRepository` bean is present.
- CSRF enabled globally; exempted paths: `/h2-console/**`,
  `/billing/webhook`, `/api/v1/**` (the mobile REST surface — see
  `docs/API.md`, which uses token auth instead of cookies).
- `SecurityConfig.securityFilterChain` is the single source of truth for
  URL-level permissions.

## Mailbox pipeline

1. `MailboxPollingScheduler` fans out `@Scheduled` polls over enabled
   `MailAccount` rows; `mailbox.polling.enabled` gates it.
2. `MailboxPollingService` opens INBOX + Sent (via
   `MailAccount.lastSeenUid` / `lastSeenSentUid` cursors), parses each
   RFC 822 with Jakarta Mail, cleans bodies, and calls
   `ThreadBuilder` to place messages into `EmailThread` rows keyed on
   the participant set (`conversation_key`).
3. Paid-tier accounts additionally hold an IMAP IDLE connection through
   `ImapIdleService`, gated by `mailbox.idle.enabled`.
4. New mail publishes a `NewMailEvent`; `ChatEventStream` fans it out to
   connected browsers over `GET /chats/events` (server-sent events).

## Web ↔ API sharing

Every business action goes through a service class (`ConversationService`,
`ReplyService`, `MailAccountService`, `BillingService`, `TeamService`, …).
Both the Thymeleaf controllers and the `/api/v1/**` REST controllers call
the same service methods — never re-implement business logic in a
controller or template.

## Commands

```
./mvnw clean package                                          # build
./mvnw test                                                   # tests
JDBC_DATABASE_PASSWORD=... \
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=local     # run local
./mvnw test -Dtest=ClassName#method                            # single test
```

## Related

- `docs/API.md` — REST contract shared with the mobile client.
- `docs/WEB.md` — Thymeleaf page inventory.
- `../conexus-mail-mobile/` — sibling mobile client repo.
