# Backend architecture

Classification: BACKEND (pom.xml, Spring Boot, Flyway migrations V1..V35). Persistence is
**Spring Data JPA** (not jOOQ — deviation from the generic target stack; TBD whether to migrate).

## Modules (`com.emailmessenger.*`)
- `auth` — form login (`email`/`password`), Google OIDC, remember-me (persistent_logins), password reset, email verification, login throttling, audit (`auth_events`).
- `billing` — Stripe checkout/portal/webhook, plans (Free/Pro/Business), `PlanLimitService`.
- `email` — mailbox connect (IMAP password + Gmail OAuth), IMAP polling, IMAP IDLE (paid), MIME parsing, import, `ConversationKeyBackfill`.
- `service` — `ConversationService`, `ChatConversation`, `IMTransformService`, `EmailBodyCleaner`, `ReplyService`, `ConversationKeyService`.
- `web` — Thymeleaf controllers (`ConversationController` = /chats), `ChatEventStream` (SSE), `OutboundMessageService`, PWA/SEO controllers.
- `team`, `digest`, `admin`, `activation`, `reengagement` — teams/invites, digests, operator metrics, lifecycle emails.
- `domain` / `repository` — JPA entities and Spring Data repos.

## Schema overview (Flyway, immutable once merged)
users, subscriptions, plan_change_events, mail_accounts, participants, email_threads (conversation_key), messages (outbound flag), message_recipients, attachments (blob_ref always null), saved_searches, teams/team_members/team_invites, digest_email_preferences, password_reset_tokens, email_verifications, auth_events, persistent_logins.

## Auth model
Session cookie (JSESSIONID) + CSRF on all POSTs except `/billing/webhook` and `/h2-console/**`. Everything not in the permitAll list requires authentication. **No token/bearer auth exists yet** — required before a mobile client can call the server.

## Web vs API
Only the Thymeleaf web layer exists. The sole non-HTML responses: `/chats/events` (SSE), Stripe webhook, PWA/SEO assets. See `docs/API.md`.
