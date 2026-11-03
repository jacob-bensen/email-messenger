# WEB — Thymeleaf website

Templates: `src/main/resources/templates` (+ `fragments/`, `admin/`, `billing/`, `mailboxes/`, `team/`, `digest/`, `password/`).

| Route(s) | Controller | Template | Services |
|---|---|---|---|
| `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund` | Landing/Pricing/Demo/Legal | landing, pricing, legal | `DemoConversationService`, site properties |
| `/login`, `/register`, `/verify-email`, `/password/*` | `AuthController`, `EmailVerificationController`, `PasswordResetController` | login, register, verify-email, password/* | `UserService`, `PasswordResetService` |
| `/auth/google/start` | `OAuthStartController` | — | OIDC |
| `/chats`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/new` | `ConversationController` | chat, compose | `ChatService`, `ConversationListService`, `OutboundMessageService` |
| `POST /chats/{key}/send`, `POST /chats/new`, `POST /chats/refresh` | `ConversationController` | — | `ReplyService`, `OutboundMessageService`, `MailboxPollingService` |
| `GET /chats/events` | `ChatEventsController` | SSE | `ChatEventStream` |
| `/mailboxes/**`, `/mailboxes/gmail/**` | `MailboxController`, `GmailConnectController` | mailboxes/* | `MailAccountService`, `PlanLimitService` |
| `/searches/**` | `SavedSearchController` | — | `SavedSearchService` |
| `/account/**` | `AccountController` | account* | `AccountService` |
| `/billing/**` | `BillingController`, `BillingWebhookController` | billing/* | `BillingService`, Stripe gateways |
| `/team/invite*` | team controller | team/* | team services |
| `/admin/**` | `AdminRevenueController` | admin/* | metrics services |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | `PwaController` | offline | — |
| `/robots.txt`, `/sitemap.xml` | `SeoController` | — | — |
| legacy `/threads/**`, `/senders` | `ChatRedirectController` | redirect | — |

Shared fragments: `fragments/live-refresh.html` (SSE in-place updates for chat).
