# WEB.md — Thymeleaf website

Templates: `src/main/resources/templates`. Fragments: `fragments/{app-nav,chat-pane,cookie-banner,install-banner,live-refresh,seo,settings-nav,sw-register}.html`.

| Route | Auth | Controller | Template |
|---|---|---|---|
| `/`, `/pricing` | public | PricingController | landing, pricing |
| `/demo` | public | DemoController (DemoConversationService) | conversation |
| `/login`, `/register`, `/auth/google/start` | public | AuthController, OAuthStartController | login, register |
| `/password/forgot`, `/password/reset` | public | PasswordResetController | password/* |
| `/verify-email`, `POST /verify-email/resend` | public / user | EmailVerificationController | verify-email |
| `/privacy`, `/terms`, `/refund` | public | LegalController | legal |
| `/robots.txt`, `/sitemap.xml`, `/images/og-card.png` | public | SeoController | — |
| `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | public | PwaController | — |
| `/chats`, `/chats/new`, `/chats/{key}`, `/chats/{key}/pane`, `POST /chats/{key}/send`, `POST /chats/refresh`, `/chats/events` | user | ConversationController, ChatEventsController | chat, compose, fragments/chat-pane |
| `/threads/**`, `/senders` | user | ChatRedirectController (redirects to /chats) | — |
| `/mailboxes` (+ `/new`, `/{id}/sync`, `/{id}/signature`, `/{id}/delete`, `/preferences`), `/mailboxes/gmail/{connect,reconnect/{id},callback}` | user | MailboxController, GmailConnectController | mailboxes/* |
| `/searches`, `/searches/{id}/delete` | user | SavedSearchController | (in chat) |
| `/account`, `/account/{notifications,appearance,privacy,password,email}` | user | AccountController | account-* |
| `/billing/{checkout,portal,success,cancel,cancel-subscription}`, `/billing/webhook` | user (webhook/cancel public) | BillingController, BillingWebhookController | billing/* |
| `/team/invite`, `/team/invite/accept` | user | TeamInviteController | team/* |
| `/digest/opt-out` | public | DigestController | digest/* |
| `/admin/revenue`, `/admin/retention/win-back`, `/admin/revenue/reconcile-billing-period` | admin | AdminRevenueController | admin/* |

Chats list/pane data comes from `ConversationListService`/`ChatService` (view models `ConversationListItem`, `ChatConversation`). Live updates: SSE `/chats/events` + `fragments/live-refresh.html`.
