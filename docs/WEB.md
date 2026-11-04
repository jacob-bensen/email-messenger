# WEB.md — Thymeleaf website

Templates: `src/main/resources/templates`. Shared fragments in `templates/fragments` (e.g. `chat-pane`, `live-refresh`).
Model data is added by `NavModelAdvice` / `SiteModelAdvice`.

## Public pages
| Route | Controller | Template |
|---|---|---|
| `/` , `/pricing` | PricingController | landing, pricing |
| `/demo` | DemoController (DemoConversationService) | conversation |
| `/login`, `/register` | AuthController | login, register |
| `/auth/google/start` | OAuthStartController | redirect |
| `/password/forgot`, `/password/reset` | PasswordResetController | password/* |
| `/verify-email`, `POST /verify-email/resend` | EmailVerificationController | verify-email |
| `/privacy`, `/terms`, `/refund` | LegalController | legal |
| `/digest/opt-out` | DigestController | digest/* |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | SeoController | — |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | PwaController | — |

## Authenticated pages
| Route | Controller | Services |
|---|---|---|
| `GET /chats` (q, mailbox, filter, page) | ConversationController | ConversationListService, MailboxPollingService |
| `GET /chats/new`, `POST /chats/new` | ConversationController | OutboundMessageService |
| `GET /chats/{key}`, `GET /chats/{key}/pane` | ConversationController | ChatService (marks read) |
| `POST /chats/{key}/send` (multipart) | ConversationController | OutboundMessageService / ReplyService |
| `POST /chats/refresh` (JSON `{imported:n}`) | ConversationController | MailboxPollingService |
| `GET /chats/events` (SSE) | ChatEventsController | ChatEventStream |
| `/threads`, `/senders` | ChatRedirectController | redirect to /chats |
| `/mailboxes` (+ `/new`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`) | MailboxController | MailAccountService, PlanLimitService |
| `/mailboxes/gmail/{connect,reconnect/{id},callback}` | GmailConnectController | GmailOAuthClient |
| `/searches`, `/searches/{id}/delete` | SavedSearchController | SavedSearchService |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | AccountController | UserService |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}` | BillingController | BillingService |
| `/team/invite`, `/team/invite/accept` | TeamInviteController | TeamInviteService |
| `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | AdminRevenueController | admin metrics services (AdminAuthorizer) |
| `POST /billing/webhook` (public, Stripe signature) | BillingWebhookController | BillingService |
