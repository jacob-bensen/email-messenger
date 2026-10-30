# BACKEND.md — architecture (reverse-engineered 2026-10-30)

Spring Boot 3 / Java 21 / Maven monolith ("ConexusMail"). It serves the public
website and the signed-in web app via Thymeleaf. **It has no mobile REST API yet**
(see `API.md`, `../INTERNAL_TODO.md`).

> Stack note: persistence is **Spring Data JPA + Flyway + PostgreSQL** (H2 in tests),
> not jOOQ; there is no codegen step.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `auth` | Form login + Google OIDC, registration, email verification, password reset, account settings, login throttling, `SecurityConfig` |
| `billing` | Stripe checkout/portal/webhook, plans (Free/Pro/Business), `PlanLimitService`, trial nudges |
| `email` | IMAP/Gmail-OAuth import (`EmailImportService`, `MimeMessageParser`), polling, IMAP IDLE, credential encryption, `MailAccountService` |
| `service` | Domain services/view models: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`, records `ChatConversation`, `ConversationListItem`, `BubbleMessage`… |
| `web` | Thymeleaf controllers + web services (`ChatService`, `ConversationListService`, `OutboundMessageService`), SSE (`ChatEventStream`), PWA/SEO |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `team`, `digest`, `admin`, `activation`, `reengagement` | Teams+invites, digests, admin metrics, lifecycle emails |

## Data model (Flyway V1–V35, immutable)
User, Subscription, PlanChangeEvent, MailAccount (auth type IMAP/Gmail OAuth, polling policy,
last-seen UIDs, signature), EmailThread (owner, `conversationKey` = participant set), Message
(inbound/outbound), MessageRecipient, Participant (deduped by normalized email), Attachment
(`blob_ref` always null — bytes not stored), SavedSearch, Team/TeamMember/TeamInvite,
auth/verification/reset tokens, AuthEvent, DigestEmailPreference.

## Auth model
Session-cookie auth (form login or Google OIDC) with CSRF on (ignored only for
`/billing/webhook`). Everything not on the permitAll list in `SecurityConfig` requires login.
No token/Bearer scheme exists → a mobile client cannot authenticate today.

## Web vs. API
All "API-like" behavior is Thymeleaf controllers returning HTML/redirects; the only JSON/SSE
endpoints are listed in `API.md`. Controllers delegate to services (`ChatService`,
`ReplyService`, `OutboundMessageService`, `MailAccountService`…), so a future JSON controller
should reuse those same services (see `INTERNAL_TODO.md` for service/controller leakage found
during audit).
