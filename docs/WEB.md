# WEB.md — Thymeleaf website

Templates: `src/main/resources/templates`. Shared fragments: `fragments/`
(`app-nav`, `settings-nav`, `chat-pane`, `live-refresh`, `seo`,
`cookie-banner`, `install-banner`, `sw-register`).

| Route | Auth | Controller → services | Template |
|---|---|---|---|
| `GET /`, `/pricing` | public | `PricingController` | `landing`, `pricing` |
| `GET /demo` | public | `DemoController` → `DemoConversationService` | `chat` |
| `/login`, `/register`, `/auth/google/start` | public | `AuthController`, `OAuthStartController` → `UserService` | `login`, `register` |
| `/password/forgot`, `/password/reset` | public | `PasswordResetController` → `PasswordResetService` | `password/*` |
| `/verify-email`, `POST /verify-email/resend` | public / auth | `EmailVerificationController` | `verify-email` |
| `GET /chats` (`page,q,mailbox,filter`) | user | `ConversationController` → `ConversationListService`, `MailAccountService`, `BillingBannerService` | `chat` |
| `GET /chats/{key}`, `/chats/{key}/pane` | user | `ChatService` (marks read) | `chat`, `fragments/chat-pane` |
| `GET/POST /chats/new`, `POST /chats/{key}/send` | user | `OutboundMessageService`, `ReplyService` | `compose`, `chat` |
| `POST /chats/refresh`, `GET /chats/events` (SSE) | user | `MailboxPollingService`, `ChatEventStream` | JSON / event-stream |
| `/mailboxes/**`, `/mailboxes/gmail/**` | user | `MailboxController`, `GmailConnectController` → `MailAccountService` | `mailboxes/*` |
| `POST /searches`, `/searches/{id}/delete` | user | `SavedSearchService` | redirect |
| `/account/**` | user | `AccountController` → `AccountService` | `account*` |
| `/billing/**` | user (webhook/cancel public) | `BillingController`, `BillingWebhookController` → `BillingService` | `billing/*` |
| `/team/invite**` | user | `TeamInviteController` → `TeamInviteService` | `team/*` |
| `/admin/revenue**` | admin | `AdminRevenueController` | `admin/revenue` |
| `/privacy`, `/terms`, `/refund` | public | `LegalController` | `legal` |
| `/robots.txt`, `/sitemap.xml`, PWA (`/manifest.webmanifest`, `/sw.js`, `/offline`, icons) | public | `SeoController`, `PwaController` | — |

`/threads/**` and `/senders` redirect to `/chats` (`ChatRedirectController`).
