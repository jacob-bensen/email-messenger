# WEB — Thymeleaf website

All pages are server-rendered by controllers in `web/`, `auth/`, `billing/`, `team/`, `digest/`, `admin/`. Public = no login.

| Route | Auth | Controller | Template |
|---|---|---|---|
| `/`, `/pricing` | public | PricingController | landing, pricing |
| `/demo` | public | DemoController (DemoConversationService) | conversation |
| `/privacy` `/terms` `/refund` | public | LegalController | legal |
| `/login` `/register` `/auth/google/start` | public | AuthController, OAuthStartController | login, register |
| `/password/forgot`, `/password/reset` | public | PasswordResetController | password/* |
| `/verify-email` (+POST `/resend`) | public | EmailVerificationController | verify-email |
| `/robots.txt` `/sitemap.xml` `/images/og-card.png` | public | SeoController | — |
| `/manifest.webmanifest` `/sw.js` `/offline` `/icons/*` | public | PwaController | — |
| `/digest/opt-out` | public | DigestController | digest/opt-out |
| `/chats` (list), `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, POST `/chats/{key}/send`, POST `/chats/refresh`, GET `/chats/events` (SSE) | user | ConversationController, ChatEventsController | chat, compose, fragments/chat-pane |
| `/threads/**`, `/senders` | user | ChatRedirectController (redirects to /chats) | — |
| `/mailboxes` (+`/new`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`) | user | MailboxController | mailboxes/* |
| `/mailboxes/gmail/{connect,reconnect/{id},callback}` | user | GmailConnectController | — |
| `/searches` (POST), `/searches/{id}/delete` | user | SavedSearchController | — |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | user | AccountController | account*.html |
| `/billing/{checkout,portal,success,cancel-subscription}` | user | BillingController | billing/* |
| `/billing/cancel` | public | BillingController | — |
| `/billing/webhook` | public (Stripe signature) | BillingWebhookController | — |
| `/team/invite`, `/team/invite/accept` | user | TeamInviteController | team/* |
| `/admin/revenue`, POST `/admin/revenue/reconcile-billing-period`, POST `/admin/retention/win-back` | admin | AdminRevenueController | admin/revenue |

Shared fragments: `fragments/{app-nav,settings-nav,chat-pane,live-refresh,seo,cookie-banner,install-banner,sw-register}`.
Controllers delegate to services (`ConversationListService`, `ChatService`, `ReplyService`, `MailAccountService`, `BillingService`); keep it that way for any future `/api/v1` controllers.
