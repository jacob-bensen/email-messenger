# Backend architecture (ConexusMail)

Reverse-engineered 2026-10-05 from the code. Items marked TBD are not implemented/verified.

## Stack (actual)
Java 21, Spring Boot 3, Spring Data **JPA** (not jOOQ — no jOOQ dependency in `pom.xml`), PostgreSQL (H2 in tests),
Flyway (`src/main/resources/db/migration`, V1..V35), Thymeleaf, Maven wrapper.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `auth` | Form login, Google OIDC, registration, email verification, password reset, login throttling, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), trial nudges |
| `email` | Mailbox connect (IMAP password + Gmail OAuth), import (`EmailImportService`), polling, IMAP IDLE, credential encryption |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), conversations, replies (`ReplyService`), conversationKey |
| `web` | Thymeleaf controllers, SSE (`ChatEventStream`), PWA/SEO endpoints, mailbox controllers |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digests, lifecycle emails, operator metrics |
| `domain` / `repository` | JPA entities / Spring Data repositories |

## Auth model
Session cookie (form login or Google OIDC). CSRF enabled except `/billing/webhook` (Stripe signature verified)
and `/h2-console/**`. Everything not in the public matcher list is `authenticated()`. **No token/API-key auth
exists**, so a mobile client cannot authenticate today (see INTERNAL_TODO.md).

## Data
Core entities: `User`, `MailAccount`, `EmailThread`, `Message`, `Participant`, `MessageRecipient`, `Attachment`
(`blob_ref` always null), `Subscription`, `Team*`, `SavedSearch`. Chats are keyed by participant-set `conversationKey`.

## Web vs REST
All user-facing functionality is server-rendered Thymeleaf. The only non-HTML endpoints are SSE `/chats/events`,
PWA assets, sitemap/robots and the Stripe webhook. **There is no REST/JSON API** for a mobile client.

## Related docs
`docs/API.md` (contract — currently only a gap analysis), `docs/WEB.md` (pages), top-level `master/` holds
legacy routine notes (PLAN, BACKLOG, 500KB CHANGELOG) from an earlier routine.
