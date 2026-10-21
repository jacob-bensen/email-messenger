# WEB.md — Thymeleaf website (routes from controller annotations)

| Area | Routes | Controller |
|---|---|---|
| Marketing | `/`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund`, `/robots.txt`, `/sitemap.xml` | PricingController, DemoController, LegalController, SeoController |
| Auth | `/login`, `/register`, `/auth/google/start`, `/password/forgot`, `/password/reset`, `/verify-email`(+`/resend`), `/logout` | AuthController, OAuthStartController, PasswordResetController, EmailVerificationController |
| Chats | `/chats`, `/chats/new`, `/chats/refresh`, `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send`, `/chats/events` (SSE) | ConversationController, ChatEventsController (service: ChatService, ConversationListService, ReplyService, OutboundMessageService) |
| Legacy | `/threads`, `/threads/**`, `/senders` → redirect to `/chats` | ChatRedirectController |
| Mailboxes | `/mailboxes`, `/new`, `/{id}/sync`, `/preferences`, `/{id}/signature`, `/{id}/delete`, `/mailboxes/gmail/{connect,reconnect/{id},callback}` | MailboxController, GmailConnectController |
| Saved searches | `POST /searches`, `/searches/{id}/delete` | SavedSearchController |
| Account | `/account`, `/account/{notifications,appearance,privacy,password,email}` | AccountController |
| Billing | `/billing/{checkout,portal,success,cancel,cancel-subscription}`, `POST /billing/webhook` | BillingController, BillingWebhookController |
| Teams | `/team/invite`, `/team/invite/accept` | TeamInviteController |
| Digest | `/digest/opt-out` | DigestController |
| Admin | `/admin/revenue`, `/admin/revenue/reconcile-billing-period`, `/admin/retention/win-back` | AdminRevenueController |
| PWA | `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*` | PwaController |

Shared fragments: `templates/fragments/` (app-nav, settings-nav, chat-pane, live-refresh, seo, cookie/install banners,
sw-register). Model attributes shared by all pages: `NavModelAdvice`, `SiteModelAdvice`.
