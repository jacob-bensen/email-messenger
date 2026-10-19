# Backend architecture

Classification: BACKEND (pom.xml, Spring Boot, Flyway, Thymeleaf). Sibling client repo: `conexus-mail-mobile` (Flutter; currently a README only).

## Deviations from the generic target stack
- Persistence is **Spring Data JPA** (Hibernate), not jOOQ. No codegen step.
- Build: Maven wrapper (`./mvnw`). DB: PostgreSQL 16 prod/local, H2 in tests. Flyway migrations `V1..V35` in `src/main/resources/db/migration`.
- Branching in this environment: work happens on the session's `claude/...` branch, not `master`, and no PRs are opened.

## Package layout (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers: chats workspace, mailboxes, Gmail connect, saved searches, PWA, SEO, legal, landing; `ChatService`, `ConversationListService`, `OutboundMessageService`, `ChatEventStream` (SSE) |
| `auth` | Spring Security config, form login, Google OIDC, registration, password reset, email verification, account settings, login throttling |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`), trial conversion |
| `email` | IMAP import/polling/IDLE, Gmail OAuth, MIME parsing, mail account management, credential encryption |
| `service` | IM transform: `ConversationKeyService`, `IMTransformService`, `EmailBodyCleaner`, `ReplyService`, chat view records (`ChatConversation`, `ChatMember`, `BubbleMessage`, `SenderBubbleRun`) |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `team`, `digest`, `admin`, `activation`, `reengagement` | Teams/invites, digests, operator metrics, onboarding & win-back |

## Auth model
Session-cookie form login (`/login`, field `email`/`password`), remember-me (30d, persistent token table), optional Google OIDC (only if `GOOGLE_CLIENT_ID` set). CSRF **enabled** (ignored only for `/h2-console/**`, `/billing/webhook`). Everything except an explicit allow-list (see `SecurityConfig`) requires authentication. **There is no token/bearer auth, so a mobile client cannot authenticate today.**

## Data model overview
`User` → `MailAccount`s (IMAP password or Gmail OAuth) → `EmailThread` → `Message` (+`MessageRecipient`, `Attachment`), `Participant` deduped by normalized address. A *chat* is keyed by `conversationKey` (participant set) and merges messages across threads. Also: `Subscription`/`Plan`/`PlanChangeEvent`, `Team*`, `SavedSearch`, `AuthEvent`, token tables.

## Web ↔ API relationship
Today the website is the only first-class client. Controllers (notably `ConversationController`) contain orchestration (recipient resolution, signature appending, new-vs-reply choice) that has **not** been extracted into a service; a REST API must extract it first. See `INTERNAL_TODO.md`.
