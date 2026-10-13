# Backend architecture

Classification evidence: `pom.xml`, Spring Boot annotations, Flyway migrations
`V1..V35` under `src/main/resources/db/migration`.

> Deviation from the generic target stack: persistence is **Spring Data JPA /
> Hibernate**, not jOOQ. Flyway owns the schema (`ddl-auto` is not used for
> production). There is **no REST/JSON API yet** — see `API.md`.

## Stack
Java 21, Spring Boot 3.x, Thymeleaf, Spring Data JPA, Spring Security
(form login + optional Google OIDC), Flyway, PostgreSQL 16 (prod/local), H2
(tests), Jakarta Mail (IMAP/SMTP), Stripe. Maven wrapper (`./mvnw`).

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers, chat workspace (`ConversationController`, `ChatService`), mailbox connect, SSE (`ChatEventsController`), PWA/SEO/legal pages |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), conversation keys, `ReplyService` |
| `email` | IMAP import/polling/IDLE, Gmail OAuth, MIME parsing, credential encryption |
| `domain` / `repository` | JPA entities and Spring Data repositories |
| `auth` | Security config, registration, login throttle, password reset, email verification, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits |
| `team`, `digest`, `activation`, `reengagement`, `admin` | teams/invites, digests, lifecycle emails, operator metrics |

## Schema overview (Flyway)
Core: `users`, `subscriptions`, `mail_accounts`, `participants`,
`email_threads` (has `conversation_key`, owner), `messages`
(outbound flag), `message_recipients`, `attachments` (`blob_ref` currently
always null). Supporting: `saved_searches`, `digest_email_preferences`,
`password_reset_tokens`, `email_verifications`, `auth_events`,
`persistent_logins`, `teams`, `team_members`, `team_invites`,
`plan_change_events`. Migrations are immutable; add `V36+`.

## Auth model
Session cookie (`JSESSIONID`) + persistent remember-me; CSRF enabled (exempt:
`/billing/webhook`, `/h2-console/**`). Everything not in the permit-all list in
`auth/SecurityConfig.java` requires authentication. Controllers resolve the
user via `UserService.requireByEmail(principal.getName())`. No token/bearer auth
exists, so a mobile client cannot authenticate today.

## Web vs. API
The website is the only client. Controllers delegate to services
(`ChatService`, `ConversationListService`, `MailAccountService`,
`OutboundMessageService`, `ReplyService`, ...); a future REST layer must reuse
those services rather than controllers.
