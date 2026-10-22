# WEB.md — Thymeleaf website

Templates in `src/main/resources/templates`. Layout is per-page with shared fragments (no layout dialect).

## Fragments
`app-nav`, `settings-nav`, `chat-pane` (open conversation), `live-refresh` (SSE in-place update),
`seo`, `cookie-banner`, `install-banner`, `sw-register`.

## Pages / routes
| Route | Template | Controller → services |
|---|---|---|
| `GET /`, `/pricing` | `landing`, `pricing` | `PricingController` |
| `GET /demo` | `chat` (demo data) | `DemoController` → `DemoConversationService` |
| `GET /login`, `/register` (+POST) | `login`, `register` | `AuthController` → `UserService` |
| `/password/forgot`, `/password/reset` | `password/*` | `PasswordResetController` |
| `/verify-email` (+`/resend`) | `verify-email` | `EmailVerificationController` |
| `GET /chats` | `chat` | `ConversationController` → `ConversationListService`, `MailAccountService`, `MailboxPollingService` |
| `GET /chats/{key}`, `/pane` | `chat` / `fragments/chat-pane` | `ChatService` |
| `GET/POST /chats/new` | `compose` | `ReplyService.sendNewEmail`, `OutboundMessageService` |
| `POST /chats/{key}/send` | redirect | `ReplyService`, `OutboundMessageService` |
| `POST /chats/refresh` | redirect | `MailboxPollingService` |
| `GET /chats/events` | SSE | `ChatEventsController` → `ChatEventStream` |
| `/mailboxes/**`, `/mailboxes/gmail/**` | `mailboxes/*` | `MailboxController`, `GmailConnectController` → `MailAccountService`, `GmailOAuthClient` |
| `/searches/{id}/delete` | redirect | `SavedSearchController` |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | `account*` | `AccountController` |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}` | `billing/*` | `BillingController` → `BillingService` |
| `/team/invite[/accept]` | `team/*` | `TeamInviteController` |
| `/digest/opt-out` | `digest/opt-out` | `DigestController` |
| `/admin/revenue` (+POSTs) | `admin/revenue` | `AdminRevenueController` |
| `/privacy`, `/terms`, `/refund` | `legal` | `LegalController` |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | PWA | `PwaController` |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | SEO | `SeoController` |
| `/threads/**`, `/senders` | redirect to `/chats` | `ChatRedirectController` |
| `POST /billing/webhook` | JSON (`@RestController`) | `BillingWebhookController` |
