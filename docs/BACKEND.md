# BACKEND — architecture

Spring Boot 3.5 / Java 21 monolith ("ConexusMail"). It renders the website
with Thymeleaf and owns all persistence and business rules. **There is no
JSON REST API yet** (see `docs/API.md`); the mobile client repo
(`conexus-mail-mobile`) is currently an empty scaffold.

> Reverse-engineered from code on 2026-10-15. Items not verified are marked TBD.
> Note: persistence is **Spring Data JPA/Hibernate** (not jOOQ); schema is
> managed only by Flyway (`ddl-auto: validate`).

## Packages (`com.emailmessenger`)

| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`ConversationController` = /chats workspace, `MailboxController`, `GmailConnectController`, `PricingController`, `PwaController`, `SeoController`, legal, saved searches), SSE (`ChatEventsController`/`ChatEventStream`), view-model services (`ChatService`, `ConversationListService`, `OutboundMessageService`, `OnboardingService`) |
| `service` | Mail → IM transform: `ConversationService`, `IMTransformService`, `EmailBodyCleaner` (quote/signature stripping), `ConversationKeyService` (participant-set key), `ReplyService` (SMTP send) |
| `email` | Mailbox integration: IMAP (`JakartaImapClient`, `ImapIdleService`), polling (`MailboxPollingService/Scheduler`), `EmailImportService`, `MimeMessageParser`, Gmail OAuth (`GmailOAuthClient`), `CredentialEncryptor` |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `auth` | Form login, Google OIDC, registration, email verification, password reset, account settings, login throttling |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimits`), trial nudges |
| `team` | Team invites and membership |
| `digest`, `reengagement`, `activation` | Scheduled emails (off unless `*.enabled=true`) |
| `admin` | `/admin/**` metrics (allowlist `admin.emails`; non-admin gets 404) |

## Data model
Flyway `V1..V35` in `src/main/resources/db/migration`. Core tables/entities:
`User`, `Subscription`/`Plan`/`PlanChangeEvent`, `MailAccount` (IMAP or Gmail
OAuth; encrypted credentials), `EmailThread`, `Message`, `MessageRecipient`,
`Participant`, `Attachment` (metadata only; `blob_ref` always null),
`SavedSearch`, `Team`/`TeamMember`/`TeamInvite`, `AuthEvent`, verification and
reset tokens, `DigestEmailPreference`. A *chat* is identified by
`conversationKey` (normalized participant set), spanning threads.

## Auth model
Session cookie (form login `/login`, remember-me, or Google OIDC via
`/auth/google/start`). CSRF enabled except `/billing/webhook`. All routes
require authentication except the public allowlist in `SecurityConfig`
(landing, pricing, legal, login/register, PWA assets, SEO). Roles: user;
admin by email allowlist. **No token/bearer auth exists** — required before a
mobile client can authenticate (see `INTERNAL_TODO.md`).

## Web vs API
All behavior is reachable only through server-rendered pages and a few
form-POST/SSE endpoints. Controllers delegate to services; business rules live
in services. When a JSON API is added it MUST call the same services.
